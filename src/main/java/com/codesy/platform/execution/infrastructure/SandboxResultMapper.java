package com.codesy.platform.execution.infrastructure;

import com.codesy.platform.execution.api.dto.SandboxExecutionResult;
import com.codesy.platform.execution.api.dto.SandboxExecutionTestCaseResult;
import com.codesy.platform.execution.api.dto.SandboxProcessResult;
import com.codesy.platform.execution.exception.SandboxExecutionException;
import com.codesy.platform.problem.domain.TestCaseVisibility;
import com.codesy.platform.submission.domain.SubmissionVerdict;
import com.codesy.platform.submission.domain.TestCaseResultVerdict;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SandboxResultMapper {

    private final ObjectMapper objectMapper;

    public SandboxExecutionResult map(Path resultFile, SandboxProcessResult processResult) {
        if (!Files.exists(resultFile)) {
            throw new SandboxExecutionException(
                    "Sandbox did not produce result file: " + resultFile +
                            " (exitCode=" + processResult.exitCode() + ")"
            );
        }

        try {
            ResultDocument document = objectMapper.readValue(resultFile.toFile(), ResultDocument.class);
            SubmissionVerdict verdict = document.verdict() == null ?
                    SubmissionVerdict.INTERNAL_ERROR : document.verdict();
            List<SandboxExecutionTestCaseResult> testCaseResults = document.testCaseResults() == null
                    ? List.of() :
                    document.testCaseResults.stream().map(
                            entry -> new SandboxExecutionTestCaseResult(
                                    entry.testCaseId(),
                                    entry.ordinal(),
                                    entry.visibility(),
                                    entry.verdict(),
                                    entry.runtimeMs(),
                                    entry.memoryKb(),
                                    entry.message(),
                                    entry.actualOutput()
                            )
                    ).toList();

            return new  SandboxExecutionResult(
                    verdict,
                    document.runtimeMs(),
                    document.memoryKb(),
                    processResult.exitCode(),
                    document.executionLog(),
                    document.compilerOutput(),
                    document.stdout() != null ? document.stdout() : processResult.stdout(),
                    document.stderr() != null ? document.stderr() : processResult.stderr(),
                    testCaseResults
            );
        }  catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResultDocument(
            SubmissionVerdict verdict,
            Long runtimeMs,
            Long memoryKb,
            String executionLog,
            String compilerOutput,
            String stdout,
            String stderr,
            List<ResultTestCaseDocument> testCaseResults
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResultTestCaseDocument(
            UUID testCaseId,
            Integer ordinal,
            TestCaseVisibility visibility,
            TestCaseResultVerdict verdict,
            Long runtimeMs,
            Long memoryKb,
            String message,
            String actualOutput
    ){}
}