package com.codesy.platform.execution.application;

import com.codesy.platform.execution.domain.OutboxEvent;
import com.codesy.platform.execution.domain.OutboxStatus;
import com.codesy.platform.execution.domain.SubmissionQueuedPayload;
import com.codesy.platform.execution.infrastructure.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(name = "app.execution.producer-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final ExecutionDispatchPort executionDispatchPort;
    private final ObjectMapper objectMapper;

    @Value("${app.execution.outbox-batch-size:25}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.execution.poll-delay:3000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> events = outboxEventRepository.findAndLockByStatus(
                OutboxStatus.NEW,
                PageRequest.of(0, batchSize));
        for (OutboxEvent event : events) {
            try {
                SubmissionQueuedPayload payload = objectMapper.readValue(event.getPayload(), SubmissionQueuedPayload.class);
                executionDispatchPort.dispatch(payload);
                event.setStatus(OutboxStatus.DISPATCHED);
                event.setProcessedAt(Instant.now());
                event.setLastError(null);
            } catch (Exception exception) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(exception.getMessage());
                if (event.getAttempts() >= 5) {
                    event.setStatus(OutboxStatus.FAILED);
                }
                log.warn("Failed to relay outbox event {}: {}", event.getId(), exception.getMessage());
            }
        }
    }

    /**
     * Archives old dispatched outbox events to prevent unbounded table growth.
     * Runs daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void archiveOldEvents() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(7));
        int deleted = outboxEventRepository.deleteByStatusInAndProcessedAtBefore(
                List.of(OutboxStatus.DISPATCHED), cutoff);
        if (deleted > 0) {
            log.info("Archived {} old dispatched outbox events", deleted);
        }
    }
}