package com.codesy.platform.submission.api.dto;

import com.codesy.platform.submission.domain.ProgrammingLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSubmissionRequest(

        @Pattern(regexp = "^[a-z0-9-]+$", message = "Invalid problem slug format")
        String problemSlug,

        @NotNull
        ProgrammingLanguage language,

        @NotBlank
        @Size(max = 20000, message = "Source code too large (max 20KB)")
        String sourceCode
) {}