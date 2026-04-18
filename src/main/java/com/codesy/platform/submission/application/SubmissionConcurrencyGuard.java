package com.codesy.platform.submission.application;

import com.codesy.platform.execution.infrastructure.ExecutionBackpressureProperties;
import com.codesy.platform.shared.exception.RateLimitExceededException;
import com.codesy.platform.submission.domain.SubmissionStatus;
import com.codesy.platform.submission.infrastructure.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmissionConcurrencyGuard {

    private static final List<SubmissionStatus> IN_FLIGHT_STATUSES = List.of(
            SubmissionStatus.CREATED,
            SubmissionStatus.QUEUED,
            SubmissionStatus.RUNNING
    );

    private final SubmissionRepository submissionRepository;
    private final ExecutionBackpressureProperties backpressureProperties;

    public void assertCanAccept(UUID userId) {
        if (!backpressureProperties.isEnabled()) {
            return;
        }

        long inFlight = submissionRepository.countByUserIdAndStatusIn(userId, IN_FLIGHT_STATUSES);
        if (inFlight >= backpressureProperties.getMaxInFlightPerUser()) {
            throw new RateLimitExceededException("You already have the maximum number of in-flight submissions");
        }
    }
}