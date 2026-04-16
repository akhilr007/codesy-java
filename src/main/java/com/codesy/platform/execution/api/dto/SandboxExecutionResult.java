package com.codesy.platform.execution.api.dto;

import com.codesy.platform.submission.domain.SubmissionVerdict;

import java.util.List;
import java.util.Objects;

public record SandboxExecutionResult(
        SubmissionVerdict verdict,
        Long runtimeMs,
        Long memoryKb,
        Integer exitCode,
        String executionLog,
        String compilerOutput,
        String stdout,
        String stderr,
        List<SandboxExecutionTestCaseResult> testCaseResults
) {
    public SandboxExecutionResult {
        Objects.requireNonNull(verdict, "verdict must not be null");
        testCaseResults = testCaseResults == null ? List.of() : List.copyOf(testCaseResults);
    }
}