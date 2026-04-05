package com.codesy.platform.user.api.dto;

import java.util.UUID;

public record UserResponse(UUID id,
                           String username,
                           String email,
                           String role) {}