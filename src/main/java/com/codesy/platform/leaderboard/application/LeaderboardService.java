package com.codesy.platform.leaderboard.application;

import com.codesy.platform.leaderboard.api.dto.LeaderboardEntryResponse;
import com.codesy.platform.leaderboard.domain.LeaderboardEntry;
import com.codesy.platform.leaderboard.infrastructure.LeaderboardEntryRepository;
import com.codesy.platform.submission.domain.Submission;
import com.codesy.platform.submission.domain.SubmissionVerdict;
import com.codesy.platform.submission.infrastructure.SubmissionResultRepository;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    public static final String GLOBAL_SCOPE = "global";
    private static final String GLOBAL_KEY = "leaderboard:global";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int POINTS_PER_ACCEPTED_SUBMISSION = 100;

    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final SubmissionResultRepository submissionResultRepository;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> topGlobal(int limit) {
        int normalizedLimit = normalizeLimit(limit);
        List<LeaderboardEntry> entries = leaderboardEntryRepository.findByScopeOrderByScoreDescAcceptedSubmissionsDescUpdatedAtAscUsernameAsc(
                GLOBAL_SCOPE,
                PageRequest.of(0, normalizedLimit));

        return IntStream.range(0, entries.size())
                .mapToObj(index -> new LeaderboardEntryResponse(
                        index + 1,
                        entries.get(index).getUsername(),
                        entries.get(index).getScore(),
                        entries.get(index).getAcceptedSubmissions()))
                .toList();
    }

    @Transactional
    public void recordAcceptedSubmission(Submission submission) {
        boolean alreadySolved = submissionResultRepository.existsBySubmissionUserIdAndSubmissionProblemIdAndVerdictAndSubmissionIdNot(
                submission.getUser().getId(),
                submission.getProblem().getId(),
                SubmissionVerdict.ACCEPTED,
                submission.getId());
        if (alreadySolved) {
            return;
        }

        LeaderboardEntry entry = leaderboardEntryRepository.findByUserAndScope(submission.getUser(), GLOBAL_SCOPE)
                .orElseGet(() -> {
                    LeaderboardEntry leaderboardEntry = new LeaderboardEntry();
                    leaderboardEntry.setUser(submission.getUser());
                    leaderboardEntry.setUsername(submission.getUser().getUsername());
                    leaderboardEntry.setScope(GLOBAL_SCOPE);
                    leaderboardEntry.setScore(0);
                    leaderboardEntry.setAcceptedSubmissions(0);
                    return leaderboardEntry;
                });

        entry.setUsername(submission.getUser().getUsername());
        entry.setScore(entry.getScore() + POINTS_PER_ACCEPTED_SUBMISSION);
        entry.setAcceptedSubmissions(entry.getAcceptedSubmissions() + 1);
        entry.setLastSubmissionAt(submission.getCompletedAt() != null ? submission.getCompletedAt() : Instant.now());
        leaderboardEntryRepository.save(entry);
        updateRedisProjection(entry);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private void updateRedisProjection(LeaderboardEntry entry) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }

        try {
            redisTemplate.opsForZSet().add(GLOBAL_KEY, entry.getUsername(), entry.getScore());
        } catch (Exception exception) {
            log.warn("Failed to update Redis leaderboard projection: {}", exception.getMessage());
        }
    }
}