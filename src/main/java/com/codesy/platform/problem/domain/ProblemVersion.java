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
@Table(name = "problem_versions")
public class ProblemVersion extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Column(name = "input_format", columnDefinition = "TEXT")
    private String inputFormat;

    @Column(name = "output_format", columnDefinition = "TEXT")
    private String outputFormat;

    @Column(name = "constraints_text", columnDefinition = "TEXT")
    private String constraintsText;

    @Column(name = "time_limit_ms", nullable = false)
    private Integer timeLimitMs;

    @Column(name = "memory_limit_mb", nullable = false)
    private Integer memoryLimitMb;

    @Column(name = "java_starter_code", columnDefinition = "TEXT")
    private String javaStarterCode;

    @Column(name = "java_execution_template", columnDefinition = "TEXT")
    private String javaExecutionTemplate;

    @Column(name = "python_starter_code", columnDefinition = "TEXT")
    private String pythonStarterCode;

    @Column(name = "python_execution_template", columnDefinition = "TEXT")
    private String pythonExecutionTemplate;

    @Column(name = "cpp_starter_code", columnDefinition = "TEXT")
    private String cppStarterCode;

    @Column(name = "cpp_execution_template", columnDefinition = "TEXT")
    private String cppExecutionTemplate;

    @Column(nullable = false)
    private boolean active;
}
