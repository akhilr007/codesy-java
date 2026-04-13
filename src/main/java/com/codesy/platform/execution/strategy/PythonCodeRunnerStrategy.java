package com.codesy.platform.execution.strategy;

import com.codesy.platform.submission.domain.ProgrammingLanguage;
import org.springframework.stereotype.Component;

@Component
public class PythonCodeRunnerStrategy extends AbstractSimulatedCodeRunnerStrategy {

    @Override
    public ProgrammingLanguage supports() {
        return ProgrammingLanguage.PYTHON_3;
    }
}