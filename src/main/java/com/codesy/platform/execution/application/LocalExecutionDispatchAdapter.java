package com.codesy.platform.execution.application;

import com.codesy.platform.execution.domain.SubmissionQueuedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.execution.dispatch", havingValue = "local", matchIfMissing = true)
public class LocalExecutionDispatchAdapter implements ExecutionDispatchPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void dispatch(SubmissionQueuedPayload payload) {
        applicationEventPublisher.publishEvent(new LocalSubmissionQueuedMessage(payload.submissionId()));
    }
}