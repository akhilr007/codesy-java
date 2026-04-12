package com.codesy.platform.submission.api.dto;

import com.codesy.platform.submission.domain.SubmissionVerdict;

import java.util.List;

public record SubmissionResultResponse(
        Integer passedTests,
        Integer totalTests,
        SubmissionVerdict verdict,
        Long runtimeMs,
        Long memoryKb,
        String executionLog,
        String compilerOutput,
        List<SubmissionTestCaseResultResponse> testCaseResults
) {}