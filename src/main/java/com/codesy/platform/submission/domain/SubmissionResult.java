package com.codesy.platform.submission.domain;

import com.codesy.platform.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "submission_results")
public class SubmissionResult extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private Submission submission;

    @Column(name = "passed_tests", nullable = false)
    private Integer passedTests;

    @Column(name = "total_tests", nullable = false)
    private Integer totalTests;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private SubmissionVerdict verdict;

    @Column(name = "runtime_ms")
    private Long runtimeMs;

    @Column(name = "memory_kb")
    private Long memoryKb;

    @Column(name = "execution_log", columnDefinition = "TEXT")
    private String executionLog;

    @Column(name = "compiler_output", columnDefinition = "TEXT")
    private String compilerOutput;

    @OneToMany(mappedBy = "submissionResult", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("testCaseOrdinal ASC")
    private List<SubmissionTestResult> testResults = new ArrayList<>();
}