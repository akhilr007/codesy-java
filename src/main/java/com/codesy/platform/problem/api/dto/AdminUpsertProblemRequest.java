package com.codesy.platform.problem.api.dto;

import com.codesy.platform.problem.domain.ProblemDifficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public record AdminUpsertProblemRequest(
        @NotBlank String slug,
        @NotBlank String title,
        @NotNull ProblemDifficulty difficulty,
        Set<String> tags,
        @NotBlank String statement,
        String inputFormat,
        String outputFormat,
        String constraintsText,
        @NotNull @Min(100) Integer timeLimitMs,
        @NotNull @Min(16) Integer memoryLimitMb,
        @Valid List<TestCaseRequest> sampleTestCases,
        @NotEmpty @Valid List<TestCaseRequest> hiddenTestCases
        ){
}