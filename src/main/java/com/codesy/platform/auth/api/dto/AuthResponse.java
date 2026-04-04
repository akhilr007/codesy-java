package com.codesy.platform.auth.api.dto;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        String username,
        String role
) {}