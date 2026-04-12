package com.codesy.platform.submission.api.controller;

import com.codesy.platform.shared.api.dto.PageResponse;
import com.codesy.platform.submission.api.dto.CreateSubmissionRequest;
import com.codesy.platform.submission.api.dto.SubmissionDetailResponse;
import com.codesy.platform.submission.api.dto.SubmissionResponse;
import com.codesy.platform.submission.api.dto.SubmissionSummaryResponse;
import com.codesy.platform.submission.application.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/submissions")
@Tag(name = "Submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "create a new submission")
    public SubmissionResponse create(@Valid @RequestBody CreateSubmissionRequest request) {
        return submissionService.createSubmission(request);
    }

    @GetMapping
    @Operation(summary = "List the current user's submissions")
    public PageResponse<SubmissionSummaryResponse> mine(@ParameterObject Pageable pageable) {
        return submissionService.listMine(pageable.getPageNumber(), pageable.getPageSize());
    }

    @GetMapping("/{submissionId}")
    @Operation(summary = "Get a submission by id")
    public SubmissionDetailResponse get(@PathVariable UUID submissionId) {
        return submissionService.getMine(submissionId);
    }
}