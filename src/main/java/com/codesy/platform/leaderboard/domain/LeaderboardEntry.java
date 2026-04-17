package com.codesy.platform.leaderboard.domain;

import com.codesy.platform.shared.domain.AuditableEntity;
import com.codesy.platform.user.domain.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "leaderboard_entries")
public class LeaderboardEntry extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(nullable = false, length = 64)
    private String scope;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "accepted_submissions", nullable = false)
    private Integer acceptedSubmissions;

    @Column(name = "last_submission_at")
    private Instant lastSubmissionAt;

}