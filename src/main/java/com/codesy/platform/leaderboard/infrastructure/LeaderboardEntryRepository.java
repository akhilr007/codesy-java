package com.codesy.platform.leaderboard.infrastructure;

import com.codesy.platform.leaderboard.domain.LeaderboardEntry;
import com.codesy.platform.user.domain.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, UUID> {

    Optional<LeaderboardEntry> findByUserAndScope(AppUser user, String scope);

    java.util.List<LeaderboardEntry> findByScopeOrderByScoreDescAcceptedSubmissionsDescUpdatedAtAscUsernameAsc(
            String scope,
            Pageable pageable);
}