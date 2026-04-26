package com.codesy.platform.problem.application;

import com.codesy.platform.problem.api.dto.*;
import com.codesy.platform.problem.domain.*;
import com.codesy.platform.problem.infrastructure.*;
import com.codesy.platform.shared.api.dto.PageResponse;
import com.codesy.platform.shared.exception.ConflictException;
import com.codesy.platform.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemVersionRepository problemVersionRepository;
    private final TestCaseRepository testCaseRepository;

    @Cacheable(cacheNames = "problem-list", key = "#page + ':' + #size")
    @Transactional(readOnly = true)
    public PageResponse<ProblemSummaryResponse> listProblems(int page, int size) {
        return PageResponse.from(
                problemRepository.findAll(PageRequest.of(page, size))
                        .map(problem -> new ProblemSummaryResponse(
                                problem.getId(),
                                problem.getSlug(),
                                problem.getTitle(),
                                problem.getDifficulty(),
                                new LinkedHashSet<>(problem.getTags())
                        ))
        );
    }

    @Cacheable(cacheNames = "problem-details", key = "#slug")
    @Transactional(readOnly = true)
    public ProblemDetailResponse getProblem(String slug) {
        Problem problem = findProblem(slug);

        ProblemVersion version = problemVersionRepository
                .findByProblemIdAndActiveTrue(problem.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Active problem version not found"));

        List<VisibleTestCaseResponse> sampleCases = testCaseRepository
                .findAllByProblemVersionIdOrderByOrdinalAsc(version.getId())
                .stream()
                .filter(tc -> tc.getVisibility() == TestCaseVisibility.SAMPLE)
                .map(tc -> new VisibleTestCaseResponse(
                        tc.getOrdinal(),
                        tc.getInputData(),
                        tc.getExpectedOutput()
                ))
                .toList();

        return new ProblemDetailResponse(
                problem.getId(),
                problem.getSlug(),
                problem.getTitle(),
                problem.getDifficulty(),
                new LinkedHashSet<>(problem.getTags()),
                version.getStatement(),
                version.getInputFormat(),
                version.getOutputFormat(),
                version.getConstraintsText(),
                version.getTimeLimitMs(),
                version.getMemoryLimitMb(),
                version.getVersionNumber(),
                starterCodes(version),
                sampleCases
        );
    }

    @Transactional(readOnly = true)
    public AdminProblemEditorResponse getAdminProblem(String slug) {
        Problem problem = findProblem(slug);

        ProblemVersion version = problemVersionRepository
                .findByProblemIdAndActiveTrue(problem.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Active problem version not found"));

        List<TestCase> testCases =
                testCaseRepository.findAllByProblemVersionIdOrderByOrdinalAsc(version.getId());

        List<AdminTestCaseResponse> sampleCases = testCases.stream()
                .filter(tc -> tc.getVisibility() == TestCaseVisibility.SAMPLE)
                .map(this::mapAdminTestCase)
                .toList();

        List<AdminTestCaseResponse> hiddenCases = testCases.stream()
                .filter(tc -> tc.getVisibility() == TestCaseVisibility.HIDDEN)
                .map(this::mapAdminTestCase)
                .toList();

        return new AdminProblemEditorResponse(
                problem.getId(),
                problem.getSlug(),
                problem.getTitle(),
                problem.getDifficulty(),
                new LinkedHashSet<>(problem.getTags()),
                version.getStatement(),
                version.getInputFormat(),
                version.getOutputFormat(),
                version.getConstraintsText(),
                version.getTimeLimitMs(),
                version.getMemoryLimitMb(),
                version.getVersionNumber(),
                adminLanguageTemplates(version),
                sampleCases,
                hiddenCases
        );
    }

    @Transactional
    @CacheEvict(cacheNames = {"problem-details", "problem-list"}, allEntries = true)
    public ProblemDetailResponse createProblem(AdminUpsertProblemRequest request) {

        String normalizedSlug = normalizeSlug(request.slug());

        if (problemRepository.existsBySlug(normalizedSlug)) {
            throw new ConflictException("Problem slug already exists");
        }

        Problem problem = new Problem();
        applyProblemMetadata(problem, request, normalizedSlug);

        try {
            problemRepository.save(problem);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Problem slug already exists");
        }

        ProblemVersion version = createVersion(problem, request);

        return toDetail(problem, version);
    }

    @Transactional
    @CacheEvict(cacheNames = {"problem-details", "problem-list"}, allEntries = true)
    public ProblemDetailResponse updateProblem(String slug, AdminUpsertProblemRequest request) {

        Problem problem = findProblem(slug);
        String normalizedSlug = normalizeSlug(request.slug());

        if (!problem.getSlug().equals(normalizedSlug)
                && problemRepository.existsBySlug(normalizedSlug)) {
            throw new ConflictException("Problem slug already exists");
        }

        applyProblemMetadata(problem, request, normalizedSlug);

        try {
            problemRepository.save(problem);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Problem slug already exists");
        }

        problemVersionRepository.deactivateByProblemId(problem.getId());

        ProblemVersion version = createVersion(problem, request);

        return toDetail(problem, version);
    }

    private ProblemVersion createVersion(Problem problem, AdminUpsertProblemRequest request) {

        Integer nextVersion = problemVersionRepository
                .findTopByProblemIdOrderByVersionNumberDesc(problem.getId())
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        ProblemVersion version = new ProblemVersion();
        version.setProblem(problem);
        version.setVersionNumber(nextVersion);
        version.setStatement(request.statement());
        version.setInputFormat(request.inputFormat());
        version.setOutputFormat(request.outputFormat());
        version.setConstraintsText(request.constraintsText());
        version.setTimeLimitMs(request.timeLimitMs());
        version.setMemoryLimitMb(request.memoryLimitMb());
        version.setActive(true);

        applyLanguageTemplates(version, request.languageTemplates());

        ProblemVersion savedVersion = problemVersionRepository.save(version);

        AtomicInteger counter = new AtomicInteger(1);
        saveTestCases(savedVersion, request.sampleTestCases(), TestCaseVisibility.SAMPLE, counter);
        saveTestCases(savedVersion, request.hiddenTestCases(), TestCaseVisibility.HIDDEN, counter);

        return savedVersion;
    }

    private ProblemStarterCodesResponse starterCodes(ProblemVersion v) {
        if (v.getJavaStarterCode() == null &&
                v.getPythonStarterCode() == null &&
                v.getCppStarterCode() == null) return null;

        return new ProblemStarterCodesResponse(
                v.getJavaStarterCode(),
                v.getPythonStarterCode(),
                v.getCppStarterCode()
        );
    }

    private AdminProblemLanguageTemplatesResponse adminLanguageTemplates(ProblemVersion v) {
        if (v.getJavaStarterCode() == null &&
                v.getJavaExecutionTemplate() == null &&
                v.getPythonStarterCode() == null &&
                v.getPythonExecutionTemplate() == null &&
                v.getCppStarterCode() == null &&
                v.getCppExecutionTemplate() == null) return null;

        return new AdminProblemLanguageTemplatesResponse(
                new AdminProblemLanguageTemplateResponse(
                        v.getJavaStarterCode(), v.getJavaExecutionTemplate()),
                new AdminProblemLanguageTemplateResponse(
                        v.getPythonStarterCode(), v.getPythonExecutionTemplate()),
                new AdminProblemLanguageTemplateResponse(
                        v.getCppStarterCode(), v.getCppExecutionTemplate())
        );
    }

    private AdminTestCaseResponse mapAdminTestCase(TestCase tc) {
        return new AdminTestCaseResponse(
                tc.getOrdinal(),
                tc.getInputData(),
                tc.getExpectedOutput()
        );
    }

    private void applyLanguageTemplates(ProblemVersion v,
                                        ProblemLanguageTemplatesRequest t) {

        if (t == null) return;

        assertTemplateContainsPlaceholder("java21", t.java21().executionTemplate());
        assertTemplateContainsPlaceholder("python3", t.python3().executionTemplate());
        assertTemplateContainsPlaceholder("cpp17", t.cpp17().executionTemplate());

        v.setJavaStarterCode(t.java21().starterCode());
        v.setJavaExecutionTemplate(t.java21().executionTemplate());
        v.setPythonStarterCode(t.python3().starterCode());
        v.setPythonExecutionTemplate(t.python3().executionTemplate());
        v.setCppStarterCode(t.cpp17().starterCode());
        v.setCppExecutionTemplate(t.cpp17().executionTemplate());
    }

    private void assertTemplateContainsPlaceholder(String lang, String template) {
        if (!template.contains(ProblemCodeTemplateConstants.USER_CODE_PLACEHOLDER)) {
            throw new IllegalArgumentException(
                    "Execution template for " + lang + " must contain placeholder"
            );
        }
    }

    private void saveTestCases(ProblemVersion v,
                               List<TestCaseRequest> requests,
                               TestCaseVisibility visibility,
                               AtomicInteger counter) {

        if (requests == null) return;

        List<TestCase> list = requests.stream().map(r -> {
            TestCase tc = new TestCase();
            tc.setProblemVersion(v);
            tc.setOrdinal(counter.getAndIncrement());
            tc.setInputData(r.inputData());
            tc.setExpectedOutput(r.expectedOutput());
            tc.setVisibility(visibility);
            return tc;
        }).toList();

        if (!list.isEmpty()) testCaseRepository.saveAll(list);
    }

    private ProblemDetailResponse toDetail(Problem problem, ProblemVersion version) {
        return getProblem(problem.getSlug());
    }

    private void applyProblemMetadata(Problem problem,
                                      AdminUpsertProblemRequest request,
                                      String slug) {

        problem.setSlug(slug);
        problem.setTitle(request.title().trim());
        problem.setDifficulty(request.difficulty());

        Set<String> tags = request.tags() == null ? new LinkedHashSet<>() :
                request.tags().stream()
                .filter(Objects::nonNull)
                .map(t -> t.trim().toLowerCase(Locale.ROOT))
                .filter(t -> !t.isBlank())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        problem.setTags(tags);
    }

    private Problem findProblem(String slug) {
        return problemRepository.findBySlug(normalizeSlug(slug))
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));
    }

    private String normalizeSlug(String slug) {
        return slug.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9\\-]", "");
    }
}