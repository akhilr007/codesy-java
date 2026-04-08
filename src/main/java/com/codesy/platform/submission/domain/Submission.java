package com.codesy.platform.submission.domain;

import com.codesy.platform.problem.domain.Problem;
import com.codesy.platform.problem.domain.ProblemVersion;
import com.codesy.platform.shared.domain.AuditableEntity;
import com.codesy.platform.user.domain.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "submissions")
public class Submission extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_version_id", nullable = false)
    private ProblemVersion problemVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProgrammingLanguage language;

    @Column(name = "source_code", nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubmissionStatus status;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(name = "queued_at")
    private Instant queuedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}