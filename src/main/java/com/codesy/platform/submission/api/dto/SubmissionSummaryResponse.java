package com.codesy.platform.submission.api.dto;

import com.codesy.platform.submission.domain.ProgrammingLanguage;
import com.codesy.platform.submission.domain.SubmissionStatus;
import com.codesy.platform.submission.domain.SubmissionVerdict;

import java.time.Instant;
import java.util.UUID;

public record SubmissionSummaryResponse(
        UUID id,
        String problemSlug,
        SubmissionStatus status,
        SubmissionVerdict verdict,
        Long runtimeMs,
        Long memoryKb,
        Instant createdAt
) {}