package com.codesy.platform.submission.application;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RateLimitKeyFactory {

    public String userSubmissionKey(UUID userId) {
        return "guardrails:submission:user:" + userId;
    }

    public String ipSubmissionKey(String clientIp) {
        return "guardrails:submission:ip:" + clientIp;
    }

    public String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}