package com.codesy.platform.execution.api.dto;

import com.codesy.platform.problem.domain.TestCaseVisibility;
import com.codesy.platform.submission.domain.TestCaseResultVerdict;

import java.util.Objects;
import java.util.UUID;

public record SandboxExecutionTestCaseResult(
        UUID testCaseId,
        Integer ordinal,
        TestCaseVisibility visibility,
        TestCaseResultVerdict verdict,
        Long runtimeMs,
        Long memoryKb,
        String message,
        String actualOutput
) {
    public SandboxExecutionTestCaseResult {
        Objects.requireNonNull(testCaseId, "testCaseId must not be null");
        Objects.requireNonNull(ordinal, "ordinal must not be null");
        Objects.requireNonNull(visibility, "visibility must not be null");
        Objects.requireNonNull(verdict, "verdict must not be null");
    }
}