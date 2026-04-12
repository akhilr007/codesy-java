package com.codesy.platform.submission.infrastructure;

import com.codesy.platform.submission.domain.SubmissionResult;
import com.codesy.platform.submission.domain.SubmissionTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionTestResultRepository extends JpaRepository<SubmissionTestResult, UUID> {

    List<SubmissionTestResult> findAllBySubmissionResultIdOrderByTestCaseOrdinalAsc(UUID submissionResultId);

    @Modifying
    @Transactional
    @Query(
            value = "DELETE FROM submission_test_results WHERE submission_result_id = :submissionResultId",
            nativeQuery = true
    )
    void deleteAllBySubmissionResultId(UUID submissionResultId);
}