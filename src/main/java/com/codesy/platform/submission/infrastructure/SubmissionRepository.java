package com.codesy.platform.submission.infrastructure;

import com.codesy.platform.submission.domain.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    @Query(
            value = "SELECT * FROM submissions WHERE user_id = :userId ORDER BY created_at DESC",
            countQuery = "SELECT COUNT(*) FROM submissions WHERE user_id = :userId",
            nativeQuery = true
    )
    Page<Submission> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query(
            value = "SELECT * FROM submissions WHERE id = :submissionId AND user_id = :userId",
            nativeQuery = true
    )
    Optional<Submission> findByIdAndUserId(
            @Param("submissionId") UUID submissionId,
            @Param("userId") UUID userId
    );
}