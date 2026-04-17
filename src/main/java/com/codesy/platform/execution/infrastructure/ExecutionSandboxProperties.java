package com.codesy.platform.execution.infrastructure;

import com.codesy.platform.submission.domain.ProgrammingLanguage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.execution.sandbox")
public class ExecutionSandboxProperties {

    private boolean enabled = false;

    @NotBlank
    private String provider = "docker";

    @NotNull
    private Path workingRoot = Path.of(System.getProperty("java.io.tmpdir"), "codesy-sandbox");

    private boolean disableNetwork = true;

    @NotBlank
    private String workspacePrefix = "submission";

    @NotBlank
    private String containerUser = "sandbox";

    @Valid
    @NotNull
    private Limits limits = new Limits();

    @Valid
    @NotNull
    private Images images = new Images();

    public String imageFor(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA_21 -> images.java21;
            case PYTHON_3 -> images.python3;
            case CPP_17 -> images.cpp17;
        };
    }

    @Getter
    @Setter
    public static class Limits {

        @NotNull
        private Duration compileTimeout = Duration.ofSeconds(10);

        @NotNull
        private Duration runTimeout = Duration.ofSeconds(2);

        @NotNull
        @DecimalMin("0.1")
        private BigDecimal cpuLimit = BigDecimal.ONE;

        @Min(16)
        private int memoryLimitKb = 256;

        @Min(1)
        private int maxProcesses = 64;

        @Min(64)
        private int maxOutputKb = 512;

        @Min(1)
        private int maxSourceKb = 128;
    }

    @Getter
    @Setter
    public static class Images {

        @NotBlank
        private String java21 = "codesy/sandbox-java:latest";

        @NotBlank
        private String python3 = "codesy/sandbox-python:latest";

        @NotBlank
        private String cpp17 = "codesy/sandbox-cpp:latest";
    }
}