package com.codesy.platform.problem.api.dto;

public record AdminProblemLanguageTemplatesResponse(
        AdminProblemLanguageTemplateResponse java21,
        AdminProblemLanguageTemplateResponse python3,
        AdminProblemLanguageTemplateResponse cpp17
) {
}
