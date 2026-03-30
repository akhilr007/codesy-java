package com.codesy.platform.auth.api.dto;

public record AuthResponse(
        String accessToken,
        String username,
        String role
) {}