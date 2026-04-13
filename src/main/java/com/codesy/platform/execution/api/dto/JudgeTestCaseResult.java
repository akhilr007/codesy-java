package com.codesy.platform.execution.api.dto;

import com.codesy.platform.problem.domain.TestCaseVisibility;
import com.codesy.platform.submission.domain.TestCaseResultVerdict;

import java.util.UUID;

public record JudgeTestCaseResult(
        UUID testCaseId,
        Integer ordinal,
        TestCaseVisibility visibility,
        TestCaseResultVerdict verdict,
        Long runtimeMs,
        Long memoryKb,
        String message,
        String inputSnapshot,
        String executedOutputSnapshot,
        String actualOutput
) {
}