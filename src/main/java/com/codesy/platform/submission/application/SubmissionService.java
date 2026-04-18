package com.codesy.platform.submission.application;

import com.codesy.platform.auth.application.AuthenticatedUserProvider;
import com.codesy.platform.execution.domain.OutboxEvent;
import com.codesy.platform.execution.domain.OutboxStatus;
import com.codesy.platform.execution.domain.SubmissionQueuedPayload;
import com.codesy.platform.execution.infrastructure.OutboxEventRepository;
import com.codesy.platform.problem.domain.Problem;
import com.codesy.platform.problem.domain.ProblemVersion;
import com.codesy.platform.problem.domain.TestCaseVisibility;
import com.codesy.platform.problem.infrastructure.ProblemRepository;
import com.codesy.platform.problem.infrastructure.ProblemVersionRepository;
import com.codesy.platform.shared.api.dto.PageResponse;
import com.codesy.platform.shared.exception.ResourceNotFoundException;
import com.codesy.platform.submission.api.dto.*;
import com.codesy.platform.submission.domain.Submission;
import com.codesy.platform.submission.domain.SubmissionResult;
import com.codesy.platform.submission.domain.SubmissionStatus;
import com.codesy.platform.submission.domain.SubmissionTestResult;
import com.codesy.platform.submission.infrastructure.SubmissionRepository;
import com.codesy.platform.submission.infrastructure.SubmissionResultRepository;
import com.codesy.platform.submission.infrastructure.SubmissionTestResultRepository;
import com.codesy.platform.user.domain.AppUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionResultRepository submissionResultRepository;
    private final SubmissionTestResultRepository submissionTestResultRepository;
    private final ProblemRepository problemRepository;
    private final ProblemVersionRepository problemVersionRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final SubmissionRateLimitService submissionRateLimitService;
    private final SubmissionConcurrencyGuard submissionConcurrencyGuard;
    private final QueuePressureGuard queuePressureGuard;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    /**
     * Creates a new submission. Guardrail checks (rate limit, concurrency, queue pressure)
     * run BEFORE the transaction to avoid holding a DB connection during Redis calls.
     * Only the actual DB write is transactional.
     */
    public SubmissionResponse createSubmission(CreateSubmissionRequest request, String clientIp) {
        AppUser user = authenticatedUserProvider.getCurrentUser();

        // Guardrails run outside the transaction to avoid holding a DB connection
        // during Redis round-trips (rate limiting) and count queries (queue pressure).
        submissionRateLimitService.assertAllowed(user.getId(), clientIp);
        submissionConcurrencyGuard.assertCanAccept(user.getId());
        queuePressureGuard.assertAcceptingNewWork();

        return persistSubmission(request, user);
    }

    @Transactional
    protected SubmissionResponse persistSubmission(CreateSubmissionRequest request, AppUser user) {
        Problem problem = problemRepository.findBySlug(request.problemSlug().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));

        ProblemVersion activeVersion = problemVersionRepository.findByProblemIdAndActiveTrue(problem.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Active problem version not found"));

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setProblemVersion(activeVersion);
        submission.setLanguage(request.language());
        submission.setSourceCode(request.sourceCode());
        submission.setStatus(SubmissionStatus.QUEUED);
        submission.setCorrelationId(UUID.randomUUID());
        submission.setAttemptNo(Math.toIntExact(
                submissionRepository.countByUserIdAndProblemId(user.getId(), problem.getId()) + 1L));
        submission.setQueuedAt(Instant.now());
        Submission saved = submissionRepository.save(submission);

        outboxEventRepository.save(buildOutbox(saved));
        return toCreatedResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionSummaryResponse> listMine(int page, int size) {
        AppUser user = authenticatedUserProvider.getCurrentUser();
        var submissions = submissionRepository.findSummaryPageByUserId(user.getId(), PageRequest.of(page, size));
        List<UUID> submissionIds = submissions.getContent().stream()
                .map(SubmissionSummaryRow::id)
                .toList();

        Map<UUID, SubmissionResultSummaryRow> resultBySubmissionId = submissionIds.isEmpty()
                ? Map.of()
                : submissionResultRepository.findSummariesBySubmissionIds(submissionIds).stream()
                  .collect(toMap(SubmissionResultSummaryRow::submissionId, identity(),
                          (left, right) -> left));

        return PageResponse.from(submissions.map(submission -> toSummaryResponse(
                submission,
                resultBySubmissionId.get(submission.id())
        )));
    }

    @Transactional(readOnly = true)
    public SubmissionDetailResponse getMine(UUID submissionId) {
        AppUser user = authenticatedUserProvider.getCurrentUser();
        SubmissionDetailRow submission = submissionRepository.findDetailByIdAndUserId(submissionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        SubmissionResult result = submissionResultRepository.findBySubmissionId(submissionId)
                .orElse(null);

        List<SubmissionTestResult> testResults = result == null
                ? List.of()
                : submissionTestResultRepository.findAllBySubmissionResultIdOrderByTestCaseOrdinalAsc(result.getId());

        return toDetailResponse(submission, result, testResults);
    }

    private OutboxEvent buildOutbox(Submission submission) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("SUBMISSION");
        event.setAggregateId(submission.getId());
        event.setEventType("SUBMISSION_QUEUED");
        event.setStatus(OutboxStatus.NEW);
        event.setPayload(serialize(new SubmissionQueuedPayload(submission.getId())));
        return event;
    }

    private SubmissionResponse toCreatedResponse(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getProblem().getSlug(),
                submission.getProblemVersion().getVersionNumber(),
                submission.getLanguage(),
                submission.getStatus(),
                submission.getCorrelationId(),
                submission.getCreatedAt(),
                submission.getCompletedAt(),
                null
        );
    }

    private SubmissionSummaryResponse toSummaryResponse(SubmissionSummaryRow submission,
                                                        SubmissionResultSummaryRow result) {
        return new SubmissionSummaryResponse(
                submission.id(),
                submission.problemSlug(),
                submission.status(),
                result != null ? result.verdict() : null,
                result != null ? result.runtimeMs() : null,
                result != null ? result.memoryKb() : null,
                submission.createdAt()
        );
    }

    private SubmissionDetailResponse toDetailResponse(SubmissionDetailRow submission,
                                                      SubmissionResult result,
                                                      List<SubmissionTestResult> testResults) {
        return new SubmissionDetailResponse(
                submission.id(),
                submission.problemSlug(),
                submission.problemVersion(),
                submission.language(),
                submission.status(),
                submission.correlationId(),
                submission.createdAt(),
                submission.completedAt(),
                result == null ? null : toResultResponse(result, testResults)
        );
    }

    private SubmissionResultResponse toResultResponse(SubmissionResult result,
                                                      List<SubmissionTestResult> testResults) {
        return new SubmissionResultResponse(
                result.getPassedTests(),
                result.getTotalTests(),
                result.getVerdict(),
                result.getRuntimeMs(),
                result.getMemoryKb(),
                result.getExecutionLog(),
                result.getCompilerOutput(),
                testResults.stream()
                        .map(this::toTestCaseResponse)
                        .toList()
        );
    }

    private SubmissionTestCaseResultResponse toTestCaseResponse(SubmissionTestResult result) {
        boolean visibleToUser = result.getVisibility() == TestCaseVisibility.SAMPLE;
        return new SubmissionTestCaseResultResponse(
                result.getTestCaseOrdinal(),
                result.getVisibility(),
                result.getVerdict(),
                result.getRuntimeMs(),
                result.getMemoryKb(),
                result.getMessage(),
                visibleToUser ? result.getInputSnapshot() : null,
                visibleToUser ? result.getExpectedOutputSnapshot() : null,
                visibleToUser ? result.getActualOutput() : null
        );
    }

    private String serialize(SubmissionQueuedPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize submission event: ", exception);
        }
    }
}