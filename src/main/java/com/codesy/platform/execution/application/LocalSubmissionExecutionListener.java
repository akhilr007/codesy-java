package com.codesy.platform.execution.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.execution.dispatch", havingValue = "local", matchIfMissing = true)
public class LocalSubmissionExecutionListener {

    private final SubmissionExecutionService submissionExecutionService;

    @Async
    @EventListener
    public void onSubmissionQueued(LocalSubmissionQueuedMessage message) {
        log.info("Executing submission {} using local dispatcher", message.submissionId());
        submissionExecutionService.execute(message.submissionId());
    }
}