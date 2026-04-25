package com.codesy.platform.problem.application;

import com.codesy.platform.problem.api.dto.*;
import com.codesy.platform.problem.domain.Problem;
import com.codesy.platform.problem.domain.ProblemCodeTemplateConstants;
import com.codesy.platform.problem.domain.ProblemVersion;
import com.codesy.platform.problem.domain.TestCase;
import com.codesy.platform.problem.domain.TestCaseVisibility;
import com.codesy.platform.problem.infrastructure.ProblemRepository;
import com.codesy.platform.problem.infrastructure.ProblemVersionRepository;
import com.codesy.platform.problem.infrastructure.TestCaseRepository;
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

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemVersionRepository problemVersionRepository;
    private final TestCaseRepository testCaseRepository;

    @Cacheable(cacheNames = "problem-list", key = "#page + ':' + #size")
    @Transactional(readOnly = true)
    public PageResponse<ProblemSummaryResponse> listProblems(int page, int size) {
        return PageResponse.from(problemRepository.findAll(PageRequest.of(page, size))
                .map(problem -> new ProblemSummaryResponse(
                        problem.getId(),
                        problem.getSlug(),
                        problem.getTitle(),
                        problem.getDifficulty(),
                        new java.util.LinkedHashSet<>(problem.getTags())
                )));
    }

    @Cacheable(cacheNames = "problem-details", key = "#slug")
import com.codesy.platform.problem.domain.TestCase;
import com.codesy.platform.problem.domain.TestCaseVisibility;
import com.codesy.platform.problem.infrastructure.ProblemRepository;
import com.codesy.platform.problem.infrastructure.ProblemVersionRepository;
import com.codesy.platform.problem.infrastructure.TestCaseRepository;
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

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemVersionRepository problemVersionRepository;
    private final TestCaseRepository testCaseRepository;

    @Cacheable(cacheNames = "problem-list", key = "#page + ':' + #size")
    @Transactional(readOnly = true)
    public PageResponse<ProblemSummaryResponse> listProblems(int page, int size) {
        return PageResponse.from(problemRepository.findAll(PageRequest.of(page, size))
                .map(problem -> new ProblemSummaryResponse(
                        problem.getId(),
                        problem.getSlug(),
                        problem.getTitle(),
                        problem.getDifficulty(),
                        new java.util.LinkedHashSet<>(problem.getTags())
                )));
    }

    @Cacheable(cacheNames = "problem-details", key = "#slug")
    @Transactional(readOnly = true)
    public ProblemDetailResponse getProblem(String slug) {
        Problem problem = findProblem(slug);
        ProblemVersion version = problemVersionRepository.findByProblemIdAndActiveTrue(problem.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Active problem version not found"));

        List<VisibleTestCaseResponse> sampleCases = testCaseRepository
                .findAllByProblemVersionIdOrderByOrdinalAsc(version.getId())
                .stream()
                .filter(testCase -> testCase.getVisibility() == TestCaseVisibility.SAMPLE)
                .map(testCase -> new VisibleTestCaseResponse(testCase.getOrdinal(), testCase.getInputData(), testCase.getExpectedOutput()))
                .toList();

        return new ProblemDetailResponse(
                problem.getId(),
                problem.getSlug(),
                problem.getTitle(),
                problem.getDifficulty(),
                new java.util.LinkedHashSet<>(problem.getTags()),
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
        ProblemVersion version = problemVersionRepository.findByProblemIdAndActiveTrue(problem.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Active problem version not found"));

        List<TestCase> testCases = testCaseRepository.findAllByProblemVersionIdOrderByOrdinalAsc(version.getId());

        List<AdminTestCaseResponse> sampleCases = testCases.stream()
                .filter(testCase -> testCase.getVisibility() == TestCaseVisibility.SAMPLE)
                .map(this::mapAdminTestCase)
                .toList();

        List<AdminTestCaseResponse> hiddenCases = testCases.stream()
                .filter(testCase -> testCase.getVisibility() == TestCaseVisibility.HIDDEN)
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

        // Save problem safely (handles DB-level race)
        try {
            problemRepository.save(problem);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Problem slug already exists");
        }

        ProblemVersion version = createVersion(problem, request);

        return mapToDetail(problem, version);
    }

    @Transactional
    @CacheEvict(cacheNames = {"problem-details", "problem-list"}, allEntries = true)
    public ProblemDetailResponse updateProblem(String slug, AdminUpsertProblemRequest request) {
        Problem problem = findProblem(slug);
        String normalizedSlug = normalizeSlug(request.slug());

        boolean isSlugChanged = !problem.getSlug().equals(normalizedSlug);

        if (isSlugChanged && problemRepository.existsBySlug(normalizedSlug)) {
            throw new ConflictException("Problem slug already exists");
        }

        applyProblemMetadata(problem, request, normalizedSlug);
        // Save problem safely (handles DB constraint race)
        try {
            problemRepository.save(problem);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Problem slug already exists");
        }

        // bulk deactivate all old versions in one query
        problemVersionRepository.deactivateByProblemId(problem.getId());

        ProblemVersion version = createVersion(problem, request);

        return mapToDetail(problem, version);
    }

    private ProblemVersion createVersion(Problem problem, AdminUpsertProblemRequest request) {
        Integer nextVersion = problemVersionRepository.findTopByProblemIdOrderByVersionNumberDesc(problem.getId())
                .map(existing -> existing.getVersionNumber() + 1)
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
        applyLanguageTemplates(version, request.languageTemplates());
        version.setActive(true);
        ProblemVersion savedVersion = problemVersionRepository.save(version);
                problem.getDifficulty(),
                new java.util.LinkedHashSet<>(problem.getTags()),
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

    private ProblemStarterCodesResponse starterCodes(ProblemVersion version) {
        if (version.getJavaStarterCode() == null
                && version.getPythonStarterCode() == null
                && version.getCppStarterCode() == null) {
            return null;
        }

        return new ProblemStarterCodesResponse(
                version.getJavaStarterCode(),
                version.getPythonStarterCode(),
                version.getCppStarterCode()
        );
    }

    private AdminProblemLanguageTemplatesResponse adminLanguageTemplates(ProblemVersion version) {
        if (version.getJavaStarterCode() == null
                && version.getJavaExecutionTemplate() == null
                && version.getPythonStarterCode() == null
                && version.getPythonExecutionTemplate() == null
                && version.getCppStarterCode() == null
                && version.getCppExecutionTemplate() == null) {
            return null;
        }

        return new AdminProblemLanguageTemplatesResponse(
                new AdminProblemLanguageTemplateResponse(
                        version.getJavaStarterCode(),
                        version.getJavaExecutionTemplate()
                ),
                new AdminProblemLanguageTemplateResponse(
                        version.getPythonStarterCode(),
                        version.getPythonExecutionTemplate()
                ),
                new AdminProblemLanguageTemplateResponse(
                        version.getCppStarterCode(),
                        version.getCppExecutionTemplate()
                )
        );
    }

    private AdminTestCaseResponse mapAdminTestCase(TestCase testCase) {
        return new AdminTestCaseResponse(
                testCase.getOrdinal(),
                testCase.getInputData(),
                testCase.getExpectedOutput()
        );
    }

    private void applyLanguageTemplates(ProblemVersion version,
                                        ProblemLanguageTemplatesRequest languageTemplates) {
        if (languageTemplates == null) {
            version.setJavaStarterCode(null);
            version.setJavaExecutionTemplate(null);
            version.setPythonStarterCode(null);
            version.setPythonExecutionTemplate(null);
            version.setCppStarterCode(null);
            version.setCppExecutionTemplate(null);
            return;
        }

        assertTemplateContainsPlaceholder("java21", languageTemplates.java21().executionTemplate());
        assertTemplateContainsPlaceholder("python3", languageTemplates.python3().executionTemplate());
        assertTemplateContainsPlaceholder("cpp17", languageTemplates.cpp17().executionTemplate());

        version.setJavaStarterCode(languageTemplates.java21().starterCode());
        version.setJavaExecutionTemplate(languageTemplates.java21().executionTemplate());
        version.setPythonStarterCode(languageTemplates.python3().starterCode());
        version.setPythonExecutionTemplate(languageTemplates.python3().executionTemplate());
        version.setCppStarterCode(languageTemplates.cpp17().starterCode());
        version.setCppExecutionTemplate(languageTemplates.cpp17().executionTemplate());
    }

    private void assertTemplateContainsPlaceholder(String language,
                                                   String executionTemplate) {
        if (!executionTemplate.contains(ProblemCodeTemplateConstants.USER_CODE_PLACEHOLDER)) {
            throw new IllegalArgumentException(
                    "Execution template for " + language + " must contain "
                            + ProblemCodeTemplateConstants.USER_CODE_PLACEHOLDER
            );
        }
    }

    private void saveTestCases(ProblemVersion version, List<TestCaseRequest> requests, TestCaseVisibility visibility, java.util.concurrent.atomic.AtomicInteger ordinalCounter) {
        if (requests == null) {
            return;
        }

        List<TestCase> entities = requests.stream()
                .map(request -> {
                    TestCase testCase = new TestCase();
                    testCase.setProblemVersion(version);
                    testCase.setOrdinal(ordinalCounter.getAndIncrement());
                    testCase.setInputData(request.inputData());
                    testCase.setExpectedOutput(request.expectedOutput());
                    testCase.setVisibility(visibility);
                    return testCase;
                })
                .toList();

        if (!entities.isEmpty()) testCaseRepository.saveAll(entities);
    }

    private ProblemDetailResponse toDetail(Problem problem, ProblemVersion version) {
        return getProblem(problem.getSlug());
    }

    private void applyProblemMetadata(Problem problem, AdminUpsertProblemRequest request, String normalizedSlug) {
        problem.setSlug(normalizedSlug);
        problem.setTitle(request.title().trim());
        problem.setDifficulty(request.difficulty());
        Set<String> tags = request.tags() == null
                ? new LinkedHashSet<>()
                : request.tags().stream()
                  .filter(Objects::nonNull)
                  .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                  .filter(tag -> !tag.isBlank())
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
