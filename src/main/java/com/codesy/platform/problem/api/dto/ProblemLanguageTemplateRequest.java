package com.codesy.platform.problem.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ProblemLanguageTemplateRequest(
        @NotBlank String starterCode,
        @NotBlank String executionTemplate
) {
}
