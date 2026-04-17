package com.codesy.platform.leaderboard.api.dto;

public record LeaderboardEntryResponse(
        int rank,
        String username,
        int score,
        int acceptedSubmissions
) {
}