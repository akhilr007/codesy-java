package com.codesy.platform.problem.infrastructure;

import com.codesy.platform.problem.domain.ProblemVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemVersionRepository extends JpaRepository<ProblemVersion, UUID> {

    Optional<ProblemVersion> findByProblemIdAndActiveTrue(UUID problemId);

    List<ProblemVersion> findAllByProblemId(UUID problemId);

    Optional<ProblemVersion> findTopByProblemIdOrderByVersionNumberDesc(UUID problemId);
}