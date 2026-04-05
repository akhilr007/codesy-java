package com.codesy.platform.problem.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TestCaseRequest(
        @NotNull @Min(1) Integer ordinal,
        @NotBlank String inputData,
        @NotBlank String expectedOutput
) {
}