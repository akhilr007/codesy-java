package com.codesy.platform.submission.application;

import com.codesy.platform.execution.infrastructure.ExecutionBackpressureProperties;
import com.codesy.platform.shared.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionRateLimitService {

    /**
     * Atomic Lua script: INCR the key and set TTL only on first creation.
     * Prevents the bug where INCR succeeds but a crash before EXPIRE
     * leaves the key with no TTL, permanently blocking the user/IP.
     */
    private static final String RATE_LIMIT_LUA = """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """;

    private static final RedisScript<Long> RATE_LIMIT_SCRIPT =
            new DefaultRedisScript<>(RATE_LIMIT_LUA, Long.class);

    private final ExecutionBackpressureProperties backpressureProperties;
    private final RateLimitKeyFactory rateLimitKeyFactory;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    private final ConcurrentHashMap<String, LocalCounter> localCounters = new ConcurrentHashMap<>();

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

    /**
     * Evicts expired local counters to prevent unbounded memory growth
     * when Redis is unavailable and the fallback map is in use.
     */
    @Scheduled(fixedDelay = 60_000)
    public void evictExpiredCounters() {
        Instant now = Instant.now();
        int before = localCounters.size();
        localCounters.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        int evicted = before - localCounters.size();
        if (evicted > 0) {
            log.debug("Evicted {} expired local rate-limit counters", evicted);
        }
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
                Long count = redisTemplate.execute(
                        RATE_LIMIT_SCRIPT,
                        List.of(key),
                        String.valueOf(window.toMillis()));
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
        Instant now = Instant.now();
        return localCounters.compute(key, (k, existing) -> {
            if (existing == null || existing.expiresAt().isBefore(now)) {
                return new LocalCounter(1L, now.plus(window));
            }
            return existing.incremented();
        }).count();
    }

    private record LocalCounter(long count, Instant expiresAt) {
        private LocalCounter incremented() {
            return new LocalCounter(count + 1L, expiresAt);
        }
    }
}