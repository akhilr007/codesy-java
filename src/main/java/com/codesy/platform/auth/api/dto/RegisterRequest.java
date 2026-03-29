package com.codesy.platform.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username cannot be empty")
        @Size(min = 3, max = 20, message = "username must be 3-20 characters")
        String username,

        @NotBlank(message = "email cannot be empty")
        @Email(message = "email must be valid")
        String email,

        @NotBlank(message = "password cannot be empty")
        @Size(min = 8, max = 50, message = "password must be 8-50 characters")
        String password
) {}