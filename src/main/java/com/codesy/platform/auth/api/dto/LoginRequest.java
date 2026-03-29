package com.codesy.platform.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "username cannot be empty")
        String username,

        @NotBlank(message = "password cannot be empty")
        @Size(min = 8, max = 50, message = "password must be 8-50 characters")
        String password
) {}