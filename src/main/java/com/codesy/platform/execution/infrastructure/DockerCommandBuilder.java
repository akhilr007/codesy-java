package com.codesy.platform.execution.infrastructure;

import com.codesy.platform.execution.api.dto.SandboxExecutionRequest;
import com.codesy.platform.execution.api.dto.SandboxFileLayout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DockerCommandBuilder {

    private static final String CONTAINER_WORKSPACE = "/workspace";

    private final ExecutionSandboxProperties properties;

    public List<String> build(SandboxExecutionRequest request) {
        SandboxFileLayout layout = request.fileLayout();
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add("--init");
        command.add("--workdir");
        command.add(CONTAINER_WORKSPACE);
        command.add("--read-only");
        command.add("--tmpfs");
        command.add("/tmp:rw,noexec,nosuid,size=64m");
        command.add("--cap-drop");
        command.add("ALL");
        command.add("--security-opt");
        command.add("no-new-privileges");
        command.add("--pids-limit");
        command.add(String.valueOf(request.limits().maxProcesses()));
        command.add("--memory");
        command.add(request.limits().memoryLimitMb()+"m");
        command.add("--cpus");
        command.add(properties.getLimits().getCpuLimit().stripTrailingZeros().toPlainString());

        if (properties.isDisableNetwork()) {
            command.add("--network");
            command.add("none");
        }
        if (properties.getContainerUser() != null && !properties.getContainerUser().isBlank()) {
            command.add("--user");
            command.add(properties.getContainerUser());
        }

        command.add("--volume");
        command.add(layout.workspaceRoot().toAbsolutePath() + ":" + CONTAINER_WORKSPACE + ":rw");

        addEnv(command, "CODESY_LANGUAGE", request.language().name());
        addEnv(command, "CODESY_REQUEST_FILE", containerPath(layout.workspaceRoot(), layout.requestFile()));
        addEnv(command, "CODESY_SOURCE_FILE", containerPath(layout.workspaceRoot(), layout.sourceFile()));
        addEnv(command, "CODESY_COMPILE_OUTPUT_FILE", containerPath(layout.workspaceRoot(), layout.compileOutputFile()));
        addEnv(command, "CODESY_STDOUT_FILE", containerPath(layout.workspaceRoot(), layout.stdoutFile()));
        addEnv(command, "CODESY_STDERR_FILE", containerPath(layout.workspaceRoot(), layout.stderrFile()));
        addEnv(command, "CODESY_RESULT_FILE", containerPath(layout.workspaceRoot(), layout.resultFile()));
        addEnv(command, "CODESY_COMPILE_TIMEOUT_MS", String.valueOf(request.limits().compileTimeout().toMillis()));
        addEnv(command, "CODESY_RUN_TIMEOUT_MS", String.valueOf(request.limits().runTimeout().toMillis()));
        addEnv(command, "CODESY_MAX_OUTPUT_KB", String.valueOf(request.limits().maxOutputKb()));
        addEnv(command, "CODESY_MAX_SOURCE_KB", String.valueOf(request.limits().maxSourceKb()));

        command.add(properties.imageFor(request.language()));
        return List.copyOf(command);

    }

    private void addEnv(List<String> command, String key, String value) {
        command.add("--env");
        command.add(key + "=" + value);
    }

    private String containerPath(Path workspaceRoot, Path hostPath) {
        Path relative = workspaceRoot.toAbsolutePath().normalize()
                .relativize(hostPath.toAbsolutePath().normalize());
        return CONTAINER_WORKSPACE + "/" + relative.toString().replace(File.separatorChar, '/');
    }
}