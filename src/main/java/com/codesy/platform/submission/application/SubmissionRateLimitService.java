package com.codesy.platform.submission.application;

import com.codesy.platform.execution.infrastructure.ExecutionBackpressureProperties;
import com.codesy.platform.shared.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionRateLimitService {

    private final ExecutionBackpressureProperties backpressureProperties;
    private final RateLimitKeyFactory rateLimitKeyFactory;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    private final Map<String, LocalCounter> localCounters = new HashMap<>();

    public void assertAllowed(UUID userId, String clientIp) {
        if (!backpressureProperties.isEnabled() ||
        !backpressureProperties.getRateLimit().isEnabled()) {
            return;
        }

        checkLimit(
                rateLimitKeyFactory.userSubmissionKey(userId),
                backpressureProperties.getRateLimit().getUserWindow(),
                backpressureProperties.getRateLimit().getMaxSubmissionsPerUser(),
                "Too many submissions from this user. Please wait a moment and try again."
        );
        checkLimit(
                rateLimitKeyFactory.ipSubmissionKey(clientIp),
                backpressureProperties.getRateLimit().getIpWindow(),
                backpressureProperties.getRateLimit().getMaxSubmissionsPerIp(),
                "Too many submissions from this IP address. Please wait a moment and try again."
        );
    }

    private void checkLimit(String key, Duration window, int maxRequests, String message) {
        long count = increment(key, window);
        if (count > maxRequests) {
            throw new RateLimitExceededException(message);
        }
    }

    private long increment(String key, Duration window) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                Long count = redisTemplate.opsForValue().increment(key);
                redisTemplate.expire(key, window);
                if (count != null) {
                    return count;
                }
            } catch (DataAccessException e) {
                log.warn("Redis-backed submission rate limiting unavailable, " +
                        "falling back to local counters {}", e.getMessage());
            }
        }
        return incrementLocally(key, window);
    }

    private long incrementLocally(String key, Duration window) {
        synchronized (localCounters) {
            Instant now = Instant.now();
            LocalCounter counter = localCounters.get(key);
            if (counter == null || counter.expiresAt().isBefore(now)) {
                counter = new LocalCounter(0L, now.plus(window));
            }
            counter = counter.incremented();
            localCounters.put(key, counter);
            return counter.count();
        }
    }


    private record LocalCounter(long count, Instant expiresAt) {
        private LocalCounter incremented() {
            return new LocalCounter(count + 1L, expiresAt);
        }
    }
}