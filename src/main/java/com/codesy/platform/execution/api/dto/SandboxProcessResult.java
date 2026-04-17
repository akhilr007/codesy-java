package com.codesy.platform.execution.api.dto;

import java.time.Duration;

public record SandboxProcessResult(
        int exitCode,
        String stdout,
        String stderr,
        boolean timedOut,
        Duration duration
) {
}