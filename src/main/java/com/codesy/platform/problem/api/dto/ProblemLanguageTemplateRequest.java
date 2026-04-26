package com.codesy.platform.problem.api.dto;

public record ProblemLanguageTemplateRequest(
        String starterCode,
        String executionTemplate
) {
}
