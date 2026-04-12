package com.codesy.platform.submission.api.dto;

import com.codesy.platform.submission.domain.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubmissionSummaryRow(
        UUID id,
        String problemSlug,
        SubmissionStatus status,
        Instant createdAt
) {}