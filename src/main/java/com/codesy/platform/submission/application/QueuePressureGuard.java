package com.codesy.platform.submission.application;

import com.codesy.platform.execution.domain.OutboxStatus;
import com.codesy.platform.execution.infrastructure.ExecutionBackpressureProperties;
import com.codesy.platform.execution.infrastructure.OutboxEventRepository;
import com.codesy.platform.shared.exception.ExecutionBackpressureException;
import com.codesy.platform.submission.domain.SubmissionStatus;
import com.codesy.platform.submission.infrastructure.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueuePressureGuard {

    private final SubmissionRepository submissionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ExecutionBackpressureProperties backpressureProperties;

    public void assertAcceptingNewWork() {
        if (!backpressureProperties.isEnabled()) {
            return;
        }

        long queueSubmissions =submissionRepository.countByStatus(SubmissionStatus.QUEUED);
        if (queueSubmissions >= backpressureProperties.getMaxQueuedSubmissions()) {
            throw new ExecutionBackpressureException("Submission queue is currently full. Please try again shortly");
        }

        long runningSubmissions = submissionRepository.countByStatus(SubmissionStatus.RUNNING);
        if (queueSubmissions >= backpressureProperties.getMaxRunningSubmissions()) {
            throw new ExecutionBackpressureException("Execution workers are saturated right now. " +
                    "Please try again shortly");
        }

        long pendingOutboxEvents = outboxEventRepository.countByStatus(OutboxStatus.NEW);
        if (pendingOutboxEvents >= backpressureProperties.getMaxPendingOutboxEvents()) {
            throw new ExecutionBackpressureException("Submission dispatch backlog is too high right now. " +
                    "Please try again shortly.");
        }
    }
}