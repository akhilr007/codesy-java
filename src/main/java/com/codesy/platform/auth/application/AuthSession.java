package com.codesy.platform.auth.application;

import com.codesy.platform.auth.api.dto.AuthResponse;

public record AuthSession(AuthResponse response, String refreshToken) {}