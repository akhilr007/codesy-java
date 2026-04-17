package com.codesy.platform.execution.strategy;

import com.codesy.platform.execution.api.dto.JudgeResult;
import com.codesy.platform.execution.api.dto.JudgeTestCaseResult;
import com.codesy.platform.problem.domain.TestCase;
import com.codesy.platform.submission.domain.Submission;
import com.codesy.platform.submission.domain.SubmissionVerdict;
import com.codesy.platform.submission.domain.TestCaseResultVerdict;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class AbstractSimulatedCodeRunnerStrategy implements CodeRunnerStrategy {

    @Override
    public JudgeResult judge(Submission submission, List<TestCase> testCases) {
        return judge(submission.getSourceCode(), testCases);
    }

    protected JudgeResult judge(String sourceCode, List<TestCase> testCases) {
        String normalized = sourceCode == null ? "" : sourceCode.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        int totalTests = testCases.size();

        if (normalized.isBlank() || lower.contains("compileerror") || lower.contains("syntaxerror")) {
            return buildCompilationError(testCases, totalTests);
        }
        if (lower.contains("timeout") || lower.contains("tle")) {
            return buildSingleFailure(
                    testCases,
                    totalTests,
                    SubmissionVerdict.TIME_LIMIT_EXCEEDED,
                    TestCaseResultVerdict.TIME_LIMIT_EXCEEDED,
                    0,
                    1_500L,
                    2_560L,
                    supports().name() + " runner exceeded the time limit",
                    null,
                    "Execution exceeded the allowed time limit");
        }
        if (lower.contains("memorylimit") || lower.contains("mle")) {
            return buildSingleFailure(
                    testCases,
                    totalTests,
                    SubmissionVerdict.MEMORY_LIMIT_EXCEEDED,
                    TestCaseResultVerdict.MEMORY_LIMIT_EXCEEDED,
                    0,
                    220L,
                    131_072L,
                    supports().name() + " runner exceeded the memory limit",
                    null,
                    "Execution exceeded the allowed memory limit");
        }
        if (lower.contains("panic") || lower.contains("throw") || lower.contains("runtimeerror")) {
            return buildSingleFailure(
                    testCases,
                    totalTests,
                    SubmissionVerdict.RUNTIME_ERROR,
                    TestCaseResultVerdict.RUNTIME_ERROR,
                    0,
                    95L,
                    3_072L,
                    supports().name() + " runner aborted with a runtime error",
                    null,
                    "Unhandled runtime exception during execution");
        }
        if (lower.contains("wronganswer") || lower.contains("todo") || lower.contains("fixme") || normalized.length() < 40) {
            int passedBeforeFailure = totalTests > 1 ? 1 : 0;
            return buildSingleFailure(
                    testCases,
                    totalTests,
                    SubmissionVerdict.WRONG_ANSWER,
                    TestCaseResultVerdict.WRONG_ANSWER,
                    passedBeforeFailure,
                    72L,
                    2_432L,
                    "Simulated " + supports().name().toLowerCase(Locale.ROOT) + " runner detected a wrong answer",
                    null,
                    "Output did not match the expected value");
        }

        return buildAccepted(testCases, totalTests, normalized);
    }

    private JudgeResult buildCompilationError(List<TestCase> testCases, int totalTests) {
        List<JudgeTestCaseResult> caseResults = testCases.stream()
                .map(testCase -> toCaseResult(
                        testCase,
                        TestCaseResultVerdict.NOT_RUN,
                        null,
                        null,
                        "Compilation failed before this test could run",
                        null))
                .toList();

        return new JudgeResult(
                SubmissionVerdict.COMPILATION_ERROR,
                0,
                totalTests,
                null,
                null,
                supports().name() + " runner rejected the submission during compilation",
                "Compilation failed: empty or invalid source code",
                caseResults);
    }

    private JudgeResult buildAccepted(List<TestCase> testCases, int totalTests, String normalized) {
        List<JudgeTestCaseResult> caseResults = new ArrayList<>();
        for (int index = 0; index < testCases.size(); index++) {
            TestCase testCase = testCases.get(index);
            long runtimeMs = 40L + supports().ordinal() * 8L + Math.min(normalized.length(), 80) + index * 3L;
            long memoryKb = 2_048L + supports().ordinal() * 384L + index * 64L;
            caseResults.add(toCaseResult(
                    testCase,
                    TestCaseResultVerdict.PASSED,
                    runtimeMs,
                    memoryKb,
                    "Test passed",
                    testCase.getExpectedOutput()));
        }

        return new JudgeResult(
                SubmissionVerdict.ACCEPTED,
                totalTests,
                totalTests,
                aggregateRuntime(caseResults),
                aggregatePeakMemory(caseResults),
                "Simulated " + supports().name().toLowerCase(Locale.ROOT) + " runner accepted the submission",
                null,
                caseResults);
    }

    private JudgeResult buildSingleFailure(List<TestCase> testCases,
                                           int totalTests,
                                           SubmissionVerdict verdict,
                                           TestCaseResultVerdict failureVerdict,
                                           int passedBeforeFailure,
                                           Long failureRuntimeMs,
                                           Long failureMemoryKb,
                                           String log,
                                           String compilerOutput,
                                           String failureMessage) {
        List<JudgeTestCaseResult> caseResults = new ArrayList<>();
        for (int index = 0; index < testCases.size(); index++) {
            TestCase testCase = testCases.get(index);
            if (index < passedBeforeFailure) {
                caseResults.add(toCaseResult(
                        testCase,
                        TestCaseResultVerdict.PASSED,
                        55L + index * 4L,
                        2_176L + index * 96L,
                        "Test passed",
                        testCase.getExpectedOutput()));
            } else if (index == passedBeforeFailure) {
                String actualOutput = failureVerdict == TestCaseResultVerdict.WRONG_ANSWER
                        ? "unexpected-output"
                        : null;
                caseResults.add(toCaseResult(
                        testCase,
                        failureVerdict,
                        failureRuntimeMs,
                        failureMemoryKb,
                        failureMessage,
                        actualOutput));
            } else {
                caseResults.add(toCaseResult(
                        testCase,
                        TestCaseResultVerdict.NOT_RUN,
                        null,
                        null,
                        "Execution stopped after an earlier failure",
                        null));
            }
        }

        return new JudgeResult(
                verdict,
                Math.min(passedBeforeFailure, totalTests),
                totalTests,
                aggregateRuntime(caseResults),
                aggregatePeakMemory(caseResults),
                log,
                compilerOutput,
                caseResults);
    }

    private JudgeTestCaseResult toCaseResult(TestCase testCase,
                                             TestCaseResultVerdict verdict,
                                             Long runtimeMs,
                                             Long memoryKb,
                                             String message,
                                             String actualOutput) {
        return new JudgeTestCaseResult(
                testCase.getId(),
                testCase.getOrdinal(),
                testCase.getVisibility(),
                verdict,
                runtimeMs,
                memoryKb,
                message,
                testCase.getInputData(),
                testCase.getExpectedOutput(),
                actualOutput
        );
    }

    private Long aggregateRuntime(List<JudgeTestCaseResult> caseResults) {
        long runtime = 0L;
        boolean hasRuntime = false;
        for (JudgeTestCaseResult caseResult : caseResults) {
            if (caseResult.runtimeMs() != null) {
                runtime += caseResult.runtimeMs();
                hasRuntime = true;
            }
        }
        return hasRuntime ? runtime : null;
    }

    private Long aggregatePeakMemory(List<JudgeTestCaseResult> caseResults) {
        long peakMemory = 0L;
        boolean hasMemory = false;
        for (JudgeTestCaseResult caseResult : caseResults) {
            if (caseResult.memoryKb() != null) {
                peakMemory = Math.max(peakMemory, caseResult.memoryKb());
                hasMemory = true;
            }
        }
        return hasMemory ? peakMemory : null;
    }
}