package com.codesy.platform.problem.domain;

import com.codesy.platform.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "test_cases")
public class TestCase extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_version_id", nullable = false)
    private ProblemVersion problemVersion;

    @Column(nullable = false)
    private Integer ordinal;

    @Column(name = "input_data", nullable = false, columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "expected_output", nullable = false, columnDefinition = "TEXT")
    private String expectedOutput;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TestCaseVisibility visibility;
}