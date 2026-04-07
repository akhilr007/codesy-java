package com.codesy.platform.problem.api.controller;

import com.codesy.platform.problem.api.dto.ProblemDetailResponse;
import com.codesy.platform.problem.api.dto.ProblemSummaryResponse;
import com.codesy.platform.problem.application.ProblemService;
import com.codesy.platform.shared.api.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
@Tag(name = "Problems")
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    @Operation(summary = "List published problems")
    public PageResponse<ProblemSummaryResponse> list(@ParameterObject Pageable pageable) {
        return problemService.listProblems(pageable.getPageNumber(), pageable.getPageSize());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get a problem by slug")
    public ProblemDetailResponse getBySlug(@PathVariable String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Slug cannot be empty");
        }
        return problemService.getProblem(slug);
    }
}