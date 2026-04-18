package com.codesy.platform.execution.infrastructure;

import com.codesy.platform.execution.domain.OutboxEvent;
import com.codesy.platform.execution.domain.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status,  Pageable pageable);

    long countByStatus(OutboxStatus status);
}