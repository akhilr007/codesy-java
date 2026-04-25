package com.codesy.platform.execution.strategy;

import com.codesy.platform.execution.application.SubmissionSourceComposer;
import com.codesy.platform.execution.infrastructure.DockerSandboxExecutor;
import com.codesy.platform.execution.infrastructure.ExecutionSandboxProperties;
import com.codesy.platform.submission.domain.ProgrammingLanguage;
import org.springframework.stereotype.Component;

@Component
public class CppCodeRunnerStrategy extends AbstractSandboxBackedCodeRunnerStrategy {


    public CppCodeRunnerStrategy(DockerSandboxExecutor sandboxExecutor,
                                 ExecutionSandboxProperties sandboxProperties,
                                 SubmissionSourceComposer submissionSourceComposer) {
        super(sandboxExecutor, sandboxProperties, submissionSourceComposer);
    }

    @Override
    public ProgrammingLanguage supports() {
        return ProgrammingLanguage.CPP_17;
    }
}
