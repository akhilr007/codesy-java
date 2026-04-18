package com.codesy.platform.execution.infrastructure;

import com.codesy.platform.execution.domain.OutboxEvent;
import com.codesy.platform.execution.domain.OutboxStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    /**
     * Acquires a row-level lock with SKIP LOCKED to prevent duplicate dispatch
     * in multi-instance deployments.
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status ORDER BY e.createdAt ASC")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<OutboxEvent> findAndLockByStatus(@Param("status") OutboxStatus status, Pageable pageable);

    long countByStatus(OutboxStatus status);

    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status IN :statuses AND e.processedAt < :cutoff")
    int deleteByStatusInAndProcessedAtBefore(@Param("statuses") List<OutboxStatus> statuses,
                                             @Param("cutoff") Instant cutoff);
}