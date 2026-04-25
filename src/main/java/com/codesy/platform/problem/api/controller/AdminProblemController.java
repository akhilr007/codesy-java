package com.codesy.platform.problem.api.controller;

import com.codesy.platform.problem.api.dto.AdminProblemEditorResponse;
import com.codesy.platform.problem.api.dto.AdminUpsertProblemRequest;
import com.codesy.platform.problem.api.dto.ProblemDetailResponse;
import com.codesy.platform.problem.application.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/problems")
@Tag(name = "Admin Problems")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProblemController {

    private final ProblemService problemService;

    @GetMapping("/{slug}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get full editable problem data for admins")
    public AdminProblemEditorResponse getProblem(@PathVariable @NotBlank String slug) {
        return problemService.getAdminProblem(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a problem and publish a new active version",
            description = "Creates a new problem with initial version and test cases"
    )
    public ProblemDetailResponse createProblem(@Valid @RequestBody AdminUpsertProblemRequest request) {
        return problemService.createProblem(request);
    }

    @PutMapping("/{slug}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update a problem by creating a new active version")
    public ProblemDetailResponse updateProblem(@PathVariable @NotBlank String slug,
                                        @Valid @RequestBody AdminUpsertProblemRequest request) {
        return problemService.updateProblem(slug, request);
    }
}
