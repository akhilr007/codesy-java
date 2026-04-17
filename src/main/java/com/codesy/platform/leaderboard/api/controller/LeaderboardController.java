package com.codesy.platform.leaderboard.api.controller;

import com.codesy.platform.leaderboard.api.dto.LeaderboardEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/leaderboard")
@Tag(name = "Leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/global")
    @Operation(summary = "Get the global leaderboard")
    public List<LeaderboardEntryResponse> global(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return leaderboardService.topGlobal(limit);
    }
}