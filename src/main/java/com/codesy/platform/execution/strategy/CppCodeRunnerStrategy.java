package com.codesy.platform.execution.strategy;

import com.codesy.platform.submission.domain.ProgrammingLanguage;
import org.springframework.stereotype.Component;

@Component
public class CppCodeRunnerStrategy extends AbstractSimulatedCodeRunnerStrategy {

    @Override
    public ProgrammingLanguage supports() {
        return ProgrammingLanguage.CPP_17;
    }
}