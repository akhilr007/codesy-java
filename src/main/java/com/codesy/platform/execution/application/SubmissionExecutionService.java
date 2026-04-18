package com.codesy.platform.execution.application;

import com.codesy.platform.execution.api.dto.JudgeResult;
import com.codesy.platform.execution.api.dto.JudgeTestCaseResult;
import com.codesy.platform.execution.factory.CodeRunnerFactory;
import com.codesy.platform.leaderboard.application.LeaderboardService;
import com.codesy.platform.problem.domain.TestCase;
import com.codesy.platform.problem.infrastructure.TestCaseRepository;
import com.codesy.platform.shared.exception.ResourceNotFoundException;
import com.codesy.platform.submission.application.SubmissionConcurrencyGuard;
import com.codesy.platform.submission.domain.*;
import com.codesy.platform.submission.infrastructure.SubmissionRepository;
import com.codesy.platform.submission.infrastructure.SubmissionResultRepository;
import com.codesy.platform.submission.infrastructure.SubmissionTestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionExecutionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionResultRepository submissionResultRepository;
    private final SubmissionTestResultRepository submissionTestResultRepository;
    private final TestCaseRepository testCaseRepository;
    private final CodeRunnerFactory codeRunnerFactory;
    private final LeaderboardService leaderboardService;
    private final SubmissionConcurrencyGuard submissionConcurrencyGuard;

    @Transactional
    public void execute(UUID submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + submissionId));
        if (isFinal(submission.getStatus())) {
            return;
        }

        submission.setStatus(SubmissionStatus.RUNNING);
        submission.setStartedAt(Instant.now());

        List<TestCase> testCases = testCaseRepository.findAllByProblemVersionIdOrderByOrdinalAsc(
                submission.getProblemVersion().getId());
        JudgeResult judgeResult;

        try {
            judgeResult = codeRunnerFactory.get(submission.getLanguage())
                    .judge(submission, testCases);
        } catch (RuntimeException exception) {
            log.error("Submission {} failed unexpectedly during judging", submissionId, exception);
            judgeResult = buildInternalErrorResult(testCases, exception);
        }

        submission.setStatus(toTerminalStatus(judgeResult.verdict()));
        submission.setCompletedAt(Instant.now());

        SubmissionResult result = submissionResultRepository.findBySubmissionId(submission.getId())
                .orElseGet(SubmissionResult::new);
        result.setSubmission(submission);
        result.setPassedTests(judgeResult.passedTests());
        result.setTotalTests(judgeResult.totalTests());
        result.setVerdict(judgeResult.verdict());
        result.setRuntimeMs(judgeResult.runtimeMs());
        result.setMemoryKb(judgeResult.memoryKb());
        result.setExecutionLog(judgeResult.executionLog());
        result.setCompilerOutput(judgeResult.compilerOutput());

        SubmissionResult savedResult = submissionResultRepository.save(result);
        submissionTestResultRepository.deleteAllBySubmissionResultId(savedResult.getId());
        submissionTestResultRepository.saveAll(toSubmissionTestResults(savedResult, judgeResult.testCaseResults()));
        submissionRepository.save(submission);

        // Release the concurrency guard slot now that this submission is terminal
        try {
            submissionConcurrencyGuard.releaseSlot(submission.getUser().getId());
        } catch (Exception e) {
            log.warn("Failed to release concurrency slot for submission {}: {}", submissionId, e.getMessage());
        }

        if (judgeResult.verdict() == SubmissionVerdict.ACCEPTED) {
            leaderboardService.recordAcceptedSubmission(submission);
        }
        log.info("Submission {} completed with verdict: {}", submissionId, judgeResult.verdict());
    }

    private boolean isFinal(SubmissionStatus status) {
        return status == SubmissionStatus.COMPLETED
                || status == SubmissionStatus.FAILED;
    }

    private SubmissionStatus toTerminalStatus(SubmissionVerdict verdict) {
        return verdict == SubmissionVerdict.INTERNAL_ERROR
                ? SubmissionStatus.FAILED
                : SubmissionStatus.COMPLETED;
    }

    private List<SubmissionTestResult> toSubmissionTestResults(SubmissionResult submissionResult,
                                                               List<JudgeTestCaseResult> judgeCaseResults) {
        List<SubmissionTestResult> testResults = new ArrayList<>();
        for (JudgeTestCaseResult judgeCaseResult : judgeCaseResults) {
            SubmissionTestResult testResult = new SubmissionTestResult();
            testResult.setSubmissionResult(submissionResult);
            testResult.setTestCaseId(judgeCaseResult.testCaseId());
            testResult.setTestCaseOrdinal(judgeCaseResult.ordinal());
            testResult.setVisibility(judgeCaseResult.visibility());
            testResult.setVerdict(judgeCaseResult.verdict());
            testResult.setRuntimeMs(judgeCaseResult.runtimeMs());
            testResult.setMemoryKb(judgeCaseResult.memoryKb());
            testResult.setMessage(judgeCaseResult.message());
            testResult.setInputSnapshot(judgeCaseResult.inputSnapshot());
            testResult.setExpectedOutputSnapshot(judgeCaseResult.expectedOutputSnapshot());
            testResult.setActualOutput(judgeCaseResult.actualOutput());
            testResults.add(testResult);
        }
        return testResults;
    }

    private JudgeResult buildInternalErrorResult(List<TestCase> testCases, RuntimeException exception) {
        List<JudgeTestCaseResult> caseResults = testCases.stream()
                .map(testCase -> new JudgeTestCaseResult(
                        testCase.getId(),
                        testCase.getOrdinal(),
                        testCase.getVisibility(),
                        TestCaseResultVerdict.NOT_RUN,
                        null,
                        null,
                        "Judge failed before this test could be completed",
                        testCase.getInputData(),
                        testCase.getExpectedOutput(),
                        null))
                .toList();

        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        return new JudgeResult(
                SubmissionVerdict.INTERNAL_ERROR,
                0,
                testCases.size(),
                null,
                null,
                "Judge execution failed unexpectedly",
                message,
                caseResults
        );
    }
}