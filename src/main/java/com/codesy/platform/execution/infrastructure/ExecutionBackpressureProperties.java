package com.codesy.platform.execution.infrastructure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.execution.guardrails")
public class ExecutionBackpressureProperties {

    private boolean enabled = true;

    @Min(1)
    private int maxInFlightPerUser = 3;

    @Min(1)
    private int maxQueuedSubmissions = 200;

    @Min(1)
    private int maxRunningSubmissions = 50;

    @Min(1)
    private int maxPendingOutboxEvents = 200;

    @Valid
    @NotNull
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class RateLimit {

        private boolean enabled = true;

        private boolean trustForwardedHeaders = false;

        @NotNull
        private Duration userWindow = Duration.ofMinutes(1);

        @Min(1)
        private int maxSubmissionsPerUser = 10;

        @NotNull
        private Duration ipWindow = Duration.ofMinutes(1);

        @Min(1)
        private int maxSubmissionsPerIp = 30;
    }
}