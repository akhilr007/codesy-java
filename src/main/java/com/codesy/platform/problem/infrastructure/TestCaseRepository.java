package com.codesy.platform.problem.infrastructure;

import com.codesy.platform.problem.domain.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    List<TestCase> findAllByProblemVersionIdOrderByOrdinalAsc(UUID problemVersionId);
}