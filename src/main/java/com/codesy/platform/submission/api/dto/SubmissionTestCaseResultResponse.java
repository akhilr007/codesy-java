package com.codesy.platform.submission.api.dto;

import com.codesy.platform.problem.domain.TestCaseVisibility;
import com.codesy.platform.submission.domain.TestCaseResultVerdict;

public record SubmissionTestCaseResultResponse(
        Integer ordinal,
        TestCaseVisibility visibility,
        TestCaseResultVerdict verdict,
        Long runtimeMs,
        Long memoryKb,
        String message,
        String inputData,
        String expectedOutput,
        String actualOutput
) {}