package com.codesy.platform.execution.application;

import com.codesy.platform.execution.domain.SubmissionQueuedPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.execution.dispatch", havingValue = "kafka")
public class KafkaExecutionDispatchAdapter implements ExecutionDispatchPort{

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.execution.topic}")
    private String topic;

    @Override
    public void dispatch(SubmissionQueuedPayload payload) {
        try {
            kafkaTemplate.send(topic, payload.submissionId().toString(), objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize submission event", e);
        }
    }
}