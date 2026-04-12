package com.codesy.platform.submission.infrastructure;

import com.codesy.platform.submission.api.dto.SubmissionResultSummaryRow;
import com.codesy.platform.submission.domain.SubmissionResult;
import com.codesy.platform.submission.domain.SubmissionVerdict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionResultRepository extends JpaRepository<SubmissionResult, UUID> {

    @Query("""
            SELECT new com.codesy.platform.submission.api.dto.SubmissionResultSummaryRow(
                result.submission.id,
                result.verdict,
                result.runtimeMs,
                result.memoryKb
            )
            FROM SubmissionResult result
            WHERE result.submission.id IN :submissionIds
            """)
    List<SubmissionResultSummaryRow> findSummariesBySubmissionIds(@Param("submissionIds") List<UUID> submissionIds);

    @Query("""
            SELECT result
            FROM SubmissionResult result
            WHERE result.submission.id = :submissionId
            """)
    Optional<SubmissionResult> findBySubmissionId(@Param("submissionId") UUID submissionId);

    boolean existsBySubmissionUserIdAndSubmissionProblemIdAndVerdictAndSubmissionIdNot(UUID userId,
                                                                                       UUID problemId,
                                                                                       SubmissionVerdict verdict,
                                                                                       UUID submissionId);
}