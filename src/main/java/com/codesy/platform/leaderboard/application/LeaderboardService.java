package com.codesy.platform.leaderboard.application;

import com.codesy.platform.leaderboard.api.dto.LeaderboardEntryResponse;
import com.codesy.platform.leaderboard.domain.LeaderboardEntry;
import com.codesy.platform.leaderboard.infrastructure.LeaderboardEntryRepository;
import com.codesy.platform.submission.domain.Submission;
import com.codesy.platform.submission.domain.SubmissionVerdict;
import com.codesy.platform.submission.infrastructure.SubmissionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    public static final String GLOBAL_SCOPE = "global";

    private static final String ZSET_KEY = "leaderboard:global";
    private static final String HASH_KEY = "leaderboard:global:stats";

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int POINTS = 100;
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final SubmissionResultRepository submissionResultRepository;
    private final ObjectProvider<StringRedisTemplate> redisProvider;

    // =========================
    // READ PATH (REDIS FIRST)
    // =========================
    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> topGlobal(int limit) {

        int normalized = normalize(limit);

        StringRedisTemplate redis = redisProvider.getIfAvailable();

        if (redis != null) {

            Set<ZSetOperations.TypedTuple<String>> ranked =
                    redis.opsForZSet()
                            .reverseRangeWithScores(ZSET_KEY, 0, normalized - 1);

            if (ranked != null && !ranked.isEmpty()) {

                log.info("[LEADERBOARD] Cache HIT size={}", ranked.size());

                List<LeaderboardEntryResponse> result = new ArrayList<>();
                int rank = 1;

                HashOperations<String, Object, Object> hash = redis.opsForHash();

                for (ZSetOperations.TypedTuple<String> entry : ranked) {

                    String username = entry.getValue();

                    Object solvedObj = hash.get(HASH_KEY, username);

                    int solved = solvedObj != null
                            ? Integer.parseInt(solvedObj.toString())
                            : 0;

                    result.add(new LeaderboardEntryResponse(
                            rank++,
                            username,
                            entry.getScore() != null ? entry.getScore().intValue() : 0,
                            solved
                    ));
                }

                return result;
            }

            log.warn("[LEADERBOARD] Cache MISS");
        }

        // =========================
        // DB FALLBACK
        // =========================
        List<LeaderboardEntry> dbEntries =
                leaderboardEntryRepository.findByScopeOrderByScoreDescAcceptedSubmissionsDescUpdatedAtAscUsernameAsc(
                        GLOBAL_SCOPE,
                        PageRequest.of(0, normalized)
                );

        log.info("[LEADERBOARD] DB fallback size={}", dbEntries.size());

        // =========================
        // REBUILD CACHE
        // =========================
        if (redis != null) {
            rebuildCache(redis, dbEntries);
        }

        return IntStream.range(0, dbEntries.size())
                .mapToObj(i -> new LeaderboardEntryResponse(
                        i + 1,
                        dbEntries.get(i).getUsername(),
                        dbEntries.get(i).getScore(),
                        dbEntries.get(i).getAcceptedSubmissions()
                ))
                .toList();
    }

    // =========================
    // WRITE PATH
    // =========================
    @Transactional
    public void recordAcceptedSubmission(Submission submission) {

        boolean alreadySolved =
                submissionResultRepository.existsBySubmissionUserIdAndSubmissionProblemIdAndVerdictAndSubmissionIdNot(
                        submission.getUser().getId(),
                        submission.getProblem().getId(),
                        SubmissionVerdict.ACCEPTED,
                        submission.getId()
                );

        if (alreadySolved) {
            log.warn("[LEADERBOARD] Duplicate ignored user={}", submission.getUser().getUsername());
            return;
        }

        LeaderboardEntry entry =
                leaderboardEntryRepository.findByUserAndScopeForUpdate(
                        submission.getUser(), GLOBAL_SCOPE
                ).orElseGet(() -> {
                    LeaderboardEntry e = new LeaderboardEntry();
                    e.setUser(submission.getUser());
                    e.setUsername(submission.getUser().getUsername());
                    e.setScope(GLOBAL_SCOPE);
                    e.setScore(0);
                    e.setAcceptedSubmissions(0);
                    return e;
                });

        entry.setScore(entry.getScore() + POINTS);
        entry.setAcceptedSubmissions(entry.getAcceptedSubmissions() + 1);
        entry.setLastSubmissionAt(
                submission.getCompletedAt() != null ? submission.getCompletedAt() : Instant.now()
        );

        leaderboardEntryRepository.save(entry);

        log.info("[LEADERBOARD] DB updated user={} score={}",
                entry.getUsername(),
                entry.getScore()
        );

        updateRedis(entry);
    }

    // =========================
    // REDIS UPDATE (DUAL STRUCTURE)
    // =========================
    private void updateRedis(LeaderboardEntry entry) {

        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) return;

        String username = entry.getUsername();

        // 1. ranking
        redis.opsForZSet().incrementScore(ZSET_KEY, username, POINTS);

        // 2. stats
        redis.opsForHash().increment(HASH_KEY, username, 1);

        // TTL safety
        redis.expire(ZSET_KEY, CACHE_TTL);
        redis.expire(HASH_KEY, CACHE_TTL);

        log.info("[LEADERBOARD] Redis updated user={} score+{} solved+1",
                username, POINTS);
    }

    // =========================
    // CACHE REBUILD
    // =========================
    private void rebuildCache(StringRedisTemplate redis, List<LeaderboardEntry> entries) {

        redis.delete(ZSET_KEY);
        redis.delete(HASH_KEY);

        for (LeaderboardEntry e : entries) {
            redis.opsForZSet().add(ZSET_KEY, e.getUsername(), e.getScore());
            redis.opsForHash().put(HASH_KEY, e.getUsername(),
                    String.valueOf(e.getAcceptedSubmissions()));
        }

        redis.expire(ZSET_KEY, CACHE_TTL);
        redis.expire(HASH_KEY, CACHE_TTL);

        log.info("[LEADERBOARD] Cache rebuilt size={}", entries.size());
    }

    // =========================
    // UTIL
    // =========================
    private int normalize(int limit) {
        if (limit <= 0) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }
}