package com.codesy.platform.problem.api.dto;

public record AdminTestCaseResponse(
        Integer ordinal,
        String inputData,
        String expectedOutput
) {
}
