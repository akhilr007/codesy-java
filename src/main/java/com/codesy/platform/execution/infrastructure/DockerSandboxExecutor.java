package com.codesy.platform.execution.infrastructure;

import com.codesy.platform.execution.api.dto.SandboxExecutionRequest;
import com.codesy.platform.execution.api.dto.SandboxExecutionRequest.ExecutionLimits;
import com.codesy.platform.execution.api.dto.SandboxExecutionResult;
import com.codesy.platform.execution.api.dto.SandboxFileLayout;
import com.codesy.platform.execution.api.dto.SandboxProcessResult;
import com.codesy.platform.execution.exception.SandboxExecutionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DockerSandboxExecutor {

    /**
     * Dedicated thread pool for reading process stdout/stderr streams.
     * Avoids stealing threads from the common ForkJoinPool, which would
     * cause starvation under concurrent sandbox executions.
     */
    private static final ExecutorService STREAM_READER_POOL =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "sandbox-stream-reader");
                t.setDaemon(true);
                return t;
            });

    private final ExecutionSandboxProperties properties;
    private final DockerCommandBuilder commandBuilder;
    private final SandboxResultMapper resultMapper;
    private final ObjectMapper objectMapper;
    private final SandboxCommandRunner commandRunner;

    @Autowired
    public DockerSandboxExecutor(ExecutionSandboxProperties properties,
                                 DockerCommandBuilder commandBuilder,
                                 SandboxResultMapper resultMapper,
                                 ObjectMapper objectMapper) {
        this(properties, commandBuilder, resultMapper, objectMapper, DockerSandboxExecutor::runCommand);
    }

    DockerSandboxExecutor(
            ExecutionSandboxProperties properties,
            DockerCommandBuilder commandBuilder,
            SandboxResultMapper resultMapper,
            ObjectMapper objectMapper,
            SandboxCommandRunner commandRunner
    ) {
        this.properties = properties;
        this.commandBuilder = commandBuilder;
        this.resultMapper = resultMapper;
        this.objectMapper = objectMapper;
        this.commandRunner = commandRunner;
    }

    public SandboxExecutionResult execute(SandboxExecutionRequest request) {
        if (!properties.isEnabled()) {
            throw new SandboxExecutionException("Sandbox execution is disabled");
        }

        prepareWorkspace(request);
        try {
            List<String> command = commandBuilder.build(request);
            log.info("Executing submission {} in docker sandbox using image {}",
                    request.submissionId(), properties.imageFor(request.language()));

            SandboxProcessResult processResult = commandRunner.run(
                    command,
                    request.fileLayout().workspaceRoot(),
                    timeoutFor(request)
            );

            if (processResult.timedOut()) {
                throw new SandboxExecutionException("Sandbox process timed out for submission " + request.submissionId());
            }

            if (!Files.exists(request.fileLayout().resultFile()) && processResult.exitCode() != 0) {
                throw new SandboxExecutionException("Sandbox process failed before producing a result for submission " +
                        request.submissionId() + ". stderr= " + processResult.stderr());
            }

            return resultMapper.map(request.fileLayout().resultFile(), processResult);
        } finally {
            cleanupWorkspace(request.fileLayout().workspaceRoot());
        }
    }

    private void prepareWorkspace(SandboxExecutionRequest request) {
        SandboxFileLayout layout = request.fileLayout();
        try {
            Files.createDirectories(layout.workspaceRoot());
            Files.writeString(layout.sourceFile(), request.sourceCode(), StandardCharsets.UTF_8);
            Files.writeString(layout.requestFile(), objectMapper.writeValueAsString(new RunnerRequestDocument(
                    request.language().name(),
                    request.limits(),
                    request.testCases()
            )), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new SandboxExecutionException("Failed to prepare sandbox workspace for submission" +
                    request.submissionId());
        }
    }

    /**
     * Recursively deletes the sandbox workspace directory after execution
     * to prevent disk space exhaustion under load.
     */
    private void cleanupWorkspace(Path workspace) {
        try {
            if (Files.exists(workspace)) {
                Files.walk(workspace)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException ignored) {
                                // Best-effort cleanup
                            }
                        });
                log.debug("Cleaned up sandbox workspace: {}", workspace);
            }
        } catch (IOException e) {
            log.warn("Failed to clean sandbox workspace {}: {}", workspace, e.getMessage());
        }
    }

    private Duration timeoutFor(SandboxExecutionRequest request) {
        ExecutionLimits limits = request.limits();
        return limits.compileTimeout()
                .plus(limits.runTimeout().multipliedBy(Math.max(1, request.testCases().size())))
                .plusSeconds(2);
    }

    private static SandboxProcessResult runCommand(List<String> command,
                                                   Path workingDirectory,
                                                   Duration timeout) {
        Instant startedAt = Instant.now();
        try {
            Process process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .start();

            CompletableFuture<String> stdout = readAsync(process.getInputStream());
            CompletableFuture<String> stderr = readAsync(process.getErrorStream());
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new SandboxProcessResult(-1, stdout.join(), stderr.join(), true,
                        Duration.between(startedAt, Instant.now()));
            }

            return new SandboxProcessResult(
                    process.exitValue(),
                    stdout.get(),
                    stderr.get(),
                    false,
                    Duration.between(startedAt, Instant.now())

            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SandboxExecutionException("Interrupted while executing docker sandbox command", exception);
        } catch (IOException | ExecutionException exception) {
            throw new SandboxExecutionException("Failed to execute docker sandbox command", exception);
        }
    }

    private static CompletableFuture<String> readAsync(InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (stream) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read sandbox process output", e);
            }
        }, STREAM_READER_POOL);
    }

    private record RunnerRequestDocument(
            String language,
            ExecutionLimits limits,
            List<SandboxExecutionRequest.TestCasePayload> testCases) {}

    @FunctionalInterface
    interface SandboxCommandRunner {
        SandboxProcessResult run(List<String> command, Path workingDirectory, Duration timeout);
    }
}