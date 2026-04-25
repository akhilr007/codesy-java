package com.codesy.platform.problem.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ProblemLanguageTemplatesRequest(
        @NotNull @Valid ProblemLanguageTemplateRequest java21,
        @NotNull @Valid ProblemLanguageTemplateRequest python3,
        @NotNull @Valid ProblemLanguageTemplateRequest cpp17
) {
}
