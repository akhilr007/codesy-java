package com.codesy.platform.problem.api.dto;

import com.codesy.platform.problem.domain.ProblemDifficulty;

import java.util.Set;
import java.util.UUID;

public record ProblemSummaryResponse (
        UUID id,
        String slug,
        String title,
        ProblemDifficulty difficulty,
        Set<String> tags
){
}