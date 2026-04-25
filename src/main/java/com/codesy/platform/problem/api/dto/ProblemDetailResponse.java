package com.codesy.platform.problem.api.dto;

import com.codesy.platform.problem.domain.ProblemDifficulty;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ProblemDetailResponse(
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
        Integer memoryLimitMs,
        Integer versionNumber,
        ProblemStarterCodesResponse starterCodes,
        List<VisibleTestCaseResponse> sampleTestCases
){
}
