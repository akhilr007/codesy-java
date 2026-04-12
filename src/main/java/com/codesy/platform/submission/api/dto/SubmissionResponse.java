package com.codesy.platform.submission.api.dto;

import com.codesy.platform.submission.domain.ProgrammingLanguage;
import com.codesy.platform.submission.domain.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        String problemSlug,
        Integer problemVersion,
        ProgrammingLanguage language,
        SubmissionStatus status,
        UUID correlationId,
        Instant createdAt,
        Instant completedAt,
        SubmissionResultResponse result
) {
}