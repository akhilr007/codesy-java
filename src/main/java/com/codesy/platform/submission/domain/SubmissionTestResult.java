package com.codesy.platform.submission.domain;

import com.codesy.platform.problem.domain.TestCaseVisibility;
import com.codesy.platform.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "submission_test_results")
public class SubmissionTestResult extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_result_id", nullable = false)
    private SubmissionResult submissionResult;

    @Column(name = "test_case_id", nullable = false)
    private UUID testCaseId;

    @Column(name = "test_case_ordinal", nullable = false)
    private Integer testCaseOrdinal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TestCaseVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private TestCaseResultVerdict verdict;

    @Column(name = "input_snapshot", nullable = false, columnDefinition = "TEXT")
    private String inputSnapshot;

    @Column(name = "expected_output_snapshot", nullable = false, columnDefinition = "TEXT")
    private String expectedOutputSnapshot;

    @Column(name = "actual_output", columnDefinition = "TEXT")
    private String actualOutput;

    @Column(name = "runtime_ms")
    private Long runtimeMs;

    @Column(name = "memory_kb")
    private Long memoryKb;

    @Column(columnDefinition = "TEXT")
    private String message;
}