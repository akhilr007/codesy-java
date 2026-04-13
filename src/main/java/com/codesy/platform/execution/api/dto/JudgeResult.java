package com.codesy.platform.execution.api.dto;

import com.codesy.platform.submission.domain.SubmissionVerdict;

import java.util.List;

public record JudgeResult(
        SubmissionVerdict verdict,
        int passedTests,
        int totalTests,
        Long runtimeMs,
        Long memoryKb,
        String executionLog,
        String compilerOutput,
        List<JudgeTestCaseResult> testCaseResults
) {
}