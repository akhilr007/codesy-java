package com.codesy.platform.submission.application;

import com.codesy.platform.execution.infrastructure.ExecutionBackpressureProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private static final String UNKNOWN_IP = "unknown";

    private final ExecutionBackpressureProperties backpressureProperties;

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_IP;
        }

        if (backpressureProperties.getRateLimit().isTrustForwardedHeaders()) {
            String forwarded = firstNonBlank(
                    request.getHeader("X-Forwarded-For"),
                    request.getHeader("X-Real-IP"),
                    request.getHeader("Forwarded")
            );
            if (forwarded != null) {
                return normalize(extractForwardedAddress(forwarded));
            }
        }

        return normalize(request.getRemoteAddr());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String extractForwardedAddress(String rowHeader) {
        String firstEntry = rowHeader.split(",")[0].trim();
        if (firstEntry.startsWith("for=")) {
            return firstEntry.substring(4).replace("\"", "");
        }
        return firstEntry;
    }

    private String normalize(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return UNKNOWN_IP;
        }
        return ipAddress.trim();
    }
}