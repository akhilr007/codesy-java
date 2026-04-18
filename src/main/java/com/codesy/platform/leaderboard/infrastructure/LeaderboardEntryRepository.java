package com.codesy.platform.leaderboard.infrastructure;

import com.codesy.platform.leaderboard.domain.LeaderboardEntry;
import com.codesy.platform.user.domain.AppUser;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, UUID> {

    Optional<LeaderboardEntry> findByUserAndScope(AppUser user, String scope);

    /**
     * Acquires a row-level lock to prevent concurrent score updates
     * from creating duplicate entries or double-counting.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM LeaderboardEntry e WHERE e.user = :user AND e.scope = :scope")
    Optional<LeaderboardEntry> findByUserAndScopeForUpdate(@Param("user") AppUser user,
                                                            @Param("scope") String scope);

    java.util.List<LeaderboardEntry> findByScopeOrderByScoreDescAcceptedSubmissionsDescUpdatedAtAscUsernameAsc(
            String scope,
            Pageable pageable);
}