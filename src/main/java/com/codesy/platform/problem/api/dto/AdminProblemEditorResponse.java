package com.codesy.platform.problem.api.dto;

import com.codesy.platform.problem.domain.ProblemDifficulty;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AdminProblemEditorResponse(
        UUID id,
        String slug,
        String title,
        ProblemDifficulty difficulty,
        Set<String> tags,
        String statement,
        String inputFormat,
        String outputFormat,
        String constraintsText,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        Integer versionNumber,
        AdminProblemLanguageTemplatesResponse languageTemplates,
        List<AdminTestCaseResponse> sampleTestCases,
        List<AdminTestCaseResponse> hiddenTestCases
) {
}
