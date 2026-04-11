package com.codesy.platform.submission.infrastructure;

import com.codesy.platform.submission.domain.SubmissionResult;
import com.codesy.platform.submission.domain.SubmissionVerdict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionResultRepository extends JpaRepository<SubmissionResult, UUID> {

    @Query(value = "SELECT * FROM submission_results WHERE submission_id = :submissionId", nativeQuery = true)
    Optional<SubmissionResult> findBySubmissionId(@Param("submissionId") UUID submissionId);

    @Query(value = """
    SELECT EXISTS(
        SELECT 1
        FROM submission_results sr
        JOIN submissions s ON sr.submission_id = s.id
        WHERE s.user_id = :userId
          AND s.problem_id = :problemId
          AND sr.verdict = :verdict
          AND s.id <> :submissionId
    )
    """, nativeQuery = true)
    boolean existsDuplicateVerdict(
            @Param("userId") UUID userId,
            @Param("problemId") UUID problemId,
            @Param("verdict") SubmissionVerdict verdict,
            @Param("submissionId") UUID submissionId
    );
}