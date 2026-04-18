package com.codesy.platform.execution.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Configures Kafka consumer error handling to prevent infinite retry loops.
 * Uses bounded retries with fixed backoff. Failed messages after all retries
 * are logged with full context for manual investigation.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.execution.dispatch", havingValue = "kafka")
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        // 3 retries with 2-second intervals, then give up
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> {
                    log.error("Kafka message processing failed after retries. " +
                                    "topic={}, partition={}, offset={}, key={}",
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            record.key(),
                            exception);
                },
                new FixedBackOff(2000L, 3L)
        );
        // Don't retry on deserialization/validation errors (they'll never succeed)
        errorHandler.addNotRetryableExceptions(
                com.fasterxml.jackson.core.JsonProcessingException.class,
                IllegalArgumentException.class
        );
        return errorHandler;
    }
}
