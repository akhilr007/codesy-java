package com.codesy.platform.submission.api.dto;

import com.codesy.platform.submission.domain.ProgrammingLanguage;
import com.codesy.platform.submission.domain.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubmissionDetailResponse(
        UUID id,
        String problemSlug,
        Instant problemVersion,
        ProgrammingLanguage language,
        SubmissionStatus status,
        UUID correlationId,
        Instant createdAt,
        Instant completedAt,
        SubmissionResultResponse result
) {}