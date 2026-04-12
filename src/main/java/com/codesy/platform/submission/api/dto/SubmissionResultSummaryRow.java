package com.codesy.platform.submission.api.dto;

import com.codesy.platform.submission.domain.SubmissionVerdict;

import java.util.UUID;

public record SubmissionResultSummaryRow(
        UUID submissionId,
        SubmissionVerdict verdict,
        Long runtimeMs,
        Long memoryKb
) {}