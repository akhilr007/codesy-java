package com.codesy.platform.execution.strategy;

import com.codesy.platform.execution.infrastructure.DockerSandboxExecutor;
import com.codesy.platform.execution.infrastructure.ExecutionSandboxProperties;
import com.codesy.platform.submission.domain.ProgrammingLanguage;
import org.springframework.stereotype.Component;

@Component
public class PythonCodeRunnerStrategy extends AbstractSandboxBackedCodeRunnerStrategy {

    public PythonCodeRunnerStrategy(DockerSandboxExecutor sandboxExecutor,
                                    ExecutionSandboxProperties sandboxProperties) {
        super(sandboxExecutor, sandboxProperties);
    }

    @Override
    public ProgrammingLanguage supports() {
        return ProgrammingLanguage.PYTHON_3;
    }
}