package com.codesy.platform.submission.application;

import com.codesy.platform.execution.infrastructure.ExecutionBackpressureProperties;
import com.codesy.platform.shared.exception.RateLimitExceededException;
import com.codesy.platform.submission.domain.SubmissionStatus;
import com.codesy.platform.submission.infrastructure.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Guards against too many concurrent in-flight submissions per user.
 * Uses Redis atomic counter when available to prevent the check-then-act
 * race condition inherent in a pure DB count approach.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionConcurrencyGuard {

    private static final List<SubmissionStatus> IN_FLIGHT_STATUSES = List.of(
            SubmissionStatus.CREATED,
            SubmissionStatus.QUEUED,
            SubmissionStatus.RUNNING
    );

    private static final Duration IN_FLIGHT_KEY_TTL = Duration.ofMinutes(30);

    private final SubmissionRepository submissionRepository;
    private final ExecutionBackpressureProperties backpressureProperties;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    /**
     * Atomically increments a per-user counter in Redis (if available).
     * Falls back to a DB count check which still has a TOCTOU window
     * but is acceptable for single-instance deployment.
     */
    public void assertCanAccept(UUID userId) {
        if (!backpressureProperties.isEnabled()) {
            return;
        }

        int maxInFlight = backpressureProperties.getMaxInFlightPerUser();
        String key = "concurrency:user:" + userId;

        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                Long current = redisTemplate.opsForValue().increment(key);
                redisTemplate.expire(key, IN_FLIGHT_KEY_TTL);
                if (current != null && current > maxInFlight) {
                    redisTemplate.opsForValue().decrement(key);
                    throw new RateLimitExceededException(
                            "You already have the maximum number of in-flight submissions");
                }
                return;
            } catch (RateLimitExceededException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Redis concurrency guard unavailable, falling back to DB check: {}",
                        e.getMessage());
            }
        }

        // Fallback: DB count (still has TOCTOU window in multi-instance, acceptable for single-instance)
        long inFlight = submissionRepository.countByUserIdAndStatusIn(userId, IN_FLIGHT_STATUSES);
        if (inFlight >= maxInFlight) {
            throw new RateLimitExceededException(
                    "You already have the maximum number of in-flight submissions");
        }
    }

    /**
     * Must be called when a submission reaches a terminal state to release the slot.
     * Called from SubmissionExecutionService after execution completes.
     */
    public void releaseSlot(UUID userId) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                String key = "concurrency:user:" + userId;
                Long result = redisTemplate.opsForValue().decrement(key);
                if (result != null && result < 0) {
                    redisTemplate.opsForValue().increment(key); // floor at 0
                }
            } catch (Exception e) {
                log.warn("Failed to release concurrency slot for user {}: {}", userId, e.getMessage());
            }
        }
    }
}