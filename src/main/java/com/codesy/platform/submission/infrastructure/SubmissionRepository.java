package com.codesy.platform.submission.infrastructure;

import com.codesy.platform.submission.api.dto.SubmissionDetailRow;
import com.codesy.platform.submission.api.dto.SubmissionSummaryRow;
import com.codesy.platform.submission.domain.Submission;
import com.codesy.platform.submission.domain.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    long countByUserIdAndProblemId(UUID userId, UUID problemId);

    long countByUserIdAndStatusIn(UUID userId, List<SubmissionStatus> statuses);

    long countByStatus(SubmissionStatus status);

    @Query(
            value = """
                    SELECT new com.codesy.platform.submission.api.dto.SubmissionSummaryRow(
                        submission.id,
                        problem.slug,
                        submission.status,
                        submission.createdAt
                    )
                    FROM Submission submission
                    JOIN submission.problem problem
                    WHERE submission.user.id = :userId
                    ORDER BY submission.createdAt DESC
                    """,
            countQuery = """
                            SELECT COUNT(submission)
                            FROM Submission submission
                            WHERE submission.user.id = :userId
                         """
    )
    Page<SubmissionSummaryRow> findSummaryPageByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
                SELECT new com.codesy.platform.submission.api.dto.SubmissionDetailRow(
                    submission.id,
                    problem.slug,
                    problemVersion.versionNumber,
                    submission.language,
                    submission.status,
                    submission.correlationId,
                    submission.createdAt,
                    submission.completedAt
                )
                FROM Submission submission
                JOIN submission.problem problem
                JOIN submission.problemVersion problemVersion
                WHERE submission.id = :submissionId
                AND submission.user.id = :userId
            """
    )
    Optional<SubmissionDetailRow> findDetailByIdAndUserId(@Param("submissionId") UUID submissionId,
                                                          @Param("userId") UUID userId);
}