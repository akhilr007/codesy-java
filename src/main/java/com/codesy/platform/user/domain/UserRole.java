package com.codesy.platform.user.domain;

import java.util.Locale;

public enum UserRole {
    USER,
    ADMIN;

    public static UserRole fromStorageValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("User role must not be blank");
        }

        try {
            return UserRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown user role: " + value, exception);
        }
    }
}