package com.codesy.platform.execution.infrastructure;

import com.codesy.platform.submission.domain.ProgrammingLanguage;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public record SandboxFileLayout(
        Path workspaceRoot,
        Path sourceFile,
        Path compileOutputFile,
        Path stdoutFile,
        Path stderrFile,
        Path resultFile
) {
    public SandboxFileLayout {
        Objects.requireNonNull(workspaceRoot, "workspaceRoot must not be null");
        Objects.requireNonNull(sourceFile, "sourceFile must not be null");
        Objects.requireNonNull(compileOutputFile, "compileOutputFile must not be null");
        Objects.requireNonNull(stdoutFile, "stdoutFile must not be null");
        Objects.requireNonNull(stderrFile, "stderrFile must not be null");
        Objects.requireNonNull(resultFile, "resultFile must not be null");
    }

    public static SandboxFileLayout forSubmission(
            ExecutionSandboxProperties properties,
            UUID submissionId,
            ProgrammingLanguage language
    ) {
        Objects.requireNonNull(properties, "properties must not be null");
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(language, "language must not be null");

        Path workspaceRoot = properties.getWorkingRoot()
                .resolve(properties.getWorkspacePrefix() + "-" + submissionId);
        return new SandboxFileLayout(
                workspaceRoot,
                workspaceRoot.resolve(sourceFileName(language)),
                workspaceRoot.resolve("compile-output.txt"),
                workspaceRoot.resolve("stdout.txt"),
                workspaceRoot.resolve("stderr.txt"),
                workspaceRoot.resolve("result.json")
        );
    }

    public static String sourceFileName(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA_21 -> "Main.java";
            case PYTHON_3 -> "main.py";
            case CPP_17 ->  "main.cpp";
        };
    }
}