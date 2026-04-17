package com.codesy.platform.execution.application;

import com.codesy.platform.execution.domain.SubmissionQueuedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.execution.dispatch", havingValue = "kafka")
public class KafkaSubmissionExecutionListener {

    private final SubmissionExecutionService submissionExecutionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.execution.topic}", autoStartup = "${app.execution.worker-enabled:false}")
    public void onSubmissionQueued(String payload) throws Exception {
        SubmissionQueuedPayload message = objectMapper.readValue(payload, SubmissionQueuedPayload.class);
        log.info("Executing submission {} from Kafka", message.submissionId());
        submissionExecutionService.execute(message.submissionId());
    }
}