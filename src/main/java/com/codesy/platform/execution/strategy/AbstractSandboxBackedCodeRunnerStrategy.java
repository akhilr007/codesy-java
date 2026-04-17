package com.codesy.platform.execution.strategy;

import com.codesy.platform.execution.api.dto.*;
import com.codesy.platform.execution.api.dto.SandboxExecutionRequest.ExecutionLimits;
import com.codesy.platform.execution.api.dto.SandboxExecutionRequest.TestCasePayload;
import com.codesy.platform.execution.infrastructure.DockerSandboxExecutor;
import com.codesy.platform.execution.infrastructure.ExecutionSandboxProperties;
import com.codesy.platform.problem.domain.ProblemVersion;
import com.codesy.platform.problem.domain.TestCase;
import com.codesy.platform.submission.domain.Submission;
import com.codesy.platform.submission.domain.TestCaseResultVerdict;

import java.time.Duration;
import java.util.*;

public abstract class AbstractSandboxBackedCodeRunnerStrategy implements CodeRunnerStrategy {

    private final DockerSandboxExecutor sandboxExecutor;
    private final ExecutionSandboxProperties sandboxProperties;

    protected AbstractSandboxBackedCodeRunnerStrategy(DockerSandboxExecutor sandboxExecutor,
                                                      ExecutionSandboxProperties sandboxProperties) {
        this.sandboxExecutor = sandboxExecutor;
        this.sandboxProperties = sandboxProperties;
    }


    @Override
    public JudgeResult judge(Submission submission, List<TestCase> testCases) {
        SandboxExecutionRequest request = new SandboxExecutionRequest(
                submission.getId(),
                supports(),
                submission.getSourceCode(),
                SandboxFileLayout.forSubmission(sandboxProperties, submission.getId(), supports()),
                toExecutionLimits(submission.getProblemVersion()),
                testCases.stream()
                        .map(this::toPayload)
                        .toList()
        );
        return toJudgeResult(testCases, sandboxExecutor.execute(request));
    }

    private ExecutionLimits toExecutionLimits(ProblemVersion problemVersion) {
        ExecutionSandboxProperties.Limits limits = sandboxProperties.getLimits();
        return new ExecutionLimits(
                limits.getCompileTimeout(),
                Duration.ofMillis(problemVersion.getTimeLimitMs()),
                problemVersion.getMemoryLimitMb(),
                limits.getMaxProcesses(),
                limits.getMaxOutputKb(),
                limits.getMaxSourceKb()
        );
    }

    private TestCasePayload toPayload(TestCase testCase) {
        return new TestCasePayload(
                testCase.getId(),
                testCase.getOrdinal(),
                testCase.getVisibility(),
                testCase.getInputData(),
                testCase.getExpectedOutput()
        );
    }

    private JudgeResult toJudgeResult(List<TestCase> testCases, SandboxExecutionResult sandboxResult) {
        Map<UUID, SandboxExecutionTestCaseResult> sandboxResultsById = new HashMap<>();
        for (SandboxExecutionTestCaseResult sandboxResultEntry : sandboxResult.testCaseResults()) {
            sandboxResultsById.put(sandboxResultEntry.testCaseId(), sandboxResultEntry);
        }

        List<JudgeTestCaseResult> judgeCaseResults = testCases.stream()
                .map(testCase -> toJudgeCaseResult(
                        testCase,
                        sandboxResultsById.get(testCase.getId()),
                        sandboxResult))
                .toList();

        int passedTests = (int) judgeCaseResults.stream()
                .filter(result -> result.verdict() == TestCaseResultVerdict.PASSED)
                .count();

        return new JudgeResult(
                sandboxResult.verdict(),
                passedTests,
                testCases.size(),
                sandboxResult.runtimeMs() != null ? sandboxResult.runtimeMs() : aggregateRuntime(judgeCaseResults),
                sandboxResult.memoryKb() != null ? sandboxResult.memoryKb() : aggregatePeakMemory(judgeCaseResults),
                sandboxResult.executionLog(),
                sandboxResult.compilerOutput(),
                judgeCaseResults
        );
    }

    private JudgeTestCaseResult toJudgeCaseResult(TestCase testCase,
                                                  SandboxExecutionTestCaseResult sandboxCaseResult,
                                                  SandboxExecutionResult sandboxResult) {
        if (sandboxCaseResult == null) {
            return new JudgeTestCaseResult(
                    testCase.getId(),
                    testCase.getOrdinal(),
                    testCase.getVisibility(),
                    defaultVerdictForMissingCase(sandboxResult),
                    null,
                    null,
                    defaultMessageForMissingCase(sandboxResult),
                    testCase.getInputData(),
                    testCase.getExpectedOutput(),
                    null
            );
        }

        return new JudgeTestCaseResult(
                testCase.getId(),
                testCase.getOrdinal(),
                testCase.getVisibility(),
                sandboxCaseResult.verdict(),
                sandboxCaseResult.runtimeMs(),
                sandboxCaseResult.memoryKb(),
                sandboxCaseResult.message(),
                testCase.getInputData(),
                testCase.getExpectedOutput(),
                sandboxCaseResult.actualOutput()
        );
    }

    private TestCaseResultVerdict defaultVerdictForMissingCase(SandboxExecutionResult sandboxResult) {
        return switch (sandboxResult.verdict()) {
            case INTERNAL_ERROR, COMPILATION_ERROR -> TestCaseResultVerdict.NOT_RUN;
            case TIME_LIMIT_EXCEEDED -> TestCaseResultVerdict.TIME_LIMIT_EXCEEDED;
            case MEMORY_LIMIT_EXCEEDED -> TestCaseResultVerdict.MEMORY_LIMIT_EXCEEDED;
            case RUNTIME_ERROR -> TestCaseResultVerdict.RUNTIME_ERROR;
            case WRONG_ANSWER -> TestCaseResultVerdict.WRONG_ANSWER;
            case ACCEPTED -> TestCaseResultVerdict.PASSED;
        };
    }

    private String defaultMessageForMissingCase(SandboxExecutionResult sandboxResult) {
        return switch (sandboxResult.verdict()) {
            case COMPILATION_ERROR -> "Compilation failed before this test could run";
            case INTERNAL_ERROR -> "Sandbox execution failed before this test could run";
            case TIME_LIMIT_EXCEEDED -> "Execution exceeded the time limit before this test could complete";
            case MEMORY_LIMIT_EXCEEDED -> "Execution exceeded the memory limit before this test could complete";
            case RUNTIME_ERROR -> "Execution stopped after a runtime error";
            case WRONG_ANSWER -> "Execution stopped after an earlier wrong answer";
            case ACCEPTED -> "Sandbox did not return a testcase result";
        };
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