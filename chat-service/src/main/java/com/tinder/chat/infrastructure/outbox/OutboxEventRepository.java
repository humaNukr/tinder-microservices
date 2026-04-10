package com.tinder.chat.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByProcessedFalse();

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.processed = true WHERE e.id = :eventId")
    void markAsProcessed(@Param("eventId") UUID eventId);
}
