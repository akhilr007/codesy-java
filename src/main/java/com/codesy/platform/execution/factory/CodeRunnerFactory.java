package com.codesy.platform.execution.factory;

import com.codesy.platform.execution.strategy.CodeRunnerStrategy;
import com.codesy.platform.shared.exception.ResourceNotFoundException;
import com.codesy.platform.submission.domain.ProgrammingLanguage;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class CodeRunnerFactory {

    private final Map<ProgrammingLanguage, CodeRunnerStrategy> strategies =
            new EnumMap<>(ProgrammingLanguage.class);

    public CodeRunnerFactory(List<CodeRunnerStrategy> strategyImplementations) {
        strategyImplementations.forEach(strategy ->
                strategies.put(strategy.supports(), strategy));
    }

    public CodeRunnerStrategy get(ProgrammingLanguage language) {
        CodeRunnerStrategy strategy = strategies.get(language);
        if (strategy == null) {
            throw new ResourceNotFoundException("No runner registered for language " + language);
        }

        return strategy;
    }
}