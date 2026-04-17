package com.codesy.platform.execution.api.dto;

import com.codesy.platform.problem.domain.TestCaseVisibility;
import com.codesy.platform.submission.domain.ProgrammingLanguage;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SandboxExecutionRequest(
        UUID submissionId,
        ProgrammingLanguage language,
        String sourceCode,
        SandboxFileLayout fileLayout,
        ExecutionLimits limits,
        List<TestCasePayload> testCases
) {

    public SandboxExecutionRequest{
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(language, "language must not be null");
        Objects.requireNonNull(sourceCode, "sourceCode must not be null");
        Objects.requireNonNull(fileLayout, "fileLayout must not be null");
        Objects.requireNonNull(limits, "limits must not be null");
        testCases = testCases == null ? List.of() : List.copyOf(testCases);
    }

    public record ExecutionLimits(
            Duration compileTimeout,
            Duration runTimeout,
            int memoryLimitMb,
            int maxProcesses,
            int maxOutputKb,
            int maxSourceKb
    ) {
        public ExecutionLimits {
            Objects.requireNonNull(compileTimeout, "compileTimeout must not be null");
            Objects.requireNonNull(runTimeout, "runTimeout must not be null");
            if (memoryLimitMb <= 0) {
                throw new IllegalArgumentException("memoryLimitMb must be greater than 0");
            }
            if (maxProcesses <= 0) {
                throw new IllegalArgumentException("maxProcesses must be greater than 0");
            }
            if (maxOutputKb <= 0) {
                throw new IllegalArgumentException("maxOutputKb must be greater than 0");
            }
            if (maxSourceKb <= 0) {
                throw new IllegalArgumentException("maxSourceKb must be greater than 0");
            }
        }
    }

    public record TestCasePayload(
            UUID testCaseId,
            Integer ordinal,
            TestCaseVisibility visibility,
            String inputData,
            String expectedOutput
    ) {
        public TestCasePayload {
            Objects.requireNonNull(testCaseId, "testCaseId must not be null");
            Objects.requireNonNull(ordinal, "ordinal must not be null");
            Objects.requireNonNull(visibility, "visibility must not be null");
            Objects.requireNonNull(inputData, "inputData must not be null");
            Objects.requireNonNull(expectedOutput, "expectedOutput must not be null");
        }
    }
}