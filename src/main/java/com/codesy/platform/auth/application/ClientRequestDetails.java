package com.codesy.platform.auth.application;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

public record ClientRequestDetails(String ipAddress, String userAgent) {
    public static ClientRequestDetails from(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ipAddress = StringUtils.hasText(forwardedFor)
                ? forwardedFor.split(",")[0].trim()
                : request.getRemoteAddr();

        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        return new ClientRequestDetails(
                truncate(ipAddress, 64, "unknown"),
                truncate(userAgent, 512, "unknown")
        );
    }

    private static String truncate(String value, int maxLength, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}