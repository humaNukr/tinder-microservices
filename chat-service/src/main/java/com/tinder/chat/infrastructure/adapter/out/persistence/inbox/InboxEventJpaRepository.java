package com.tinder.chat.infrastructure.adapter.out.persistence.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface InboxEventJpaRepository extends JpaRepository<InboxEventEntity, UUID> {
    boolean existsByEventId(UUID eventId);

    @Modifying
    @Query("DELETE FROM InboxEventEntity i WHERE i.processedAt < :threshold")
    int deleteOlderThan(@Param("threshold") Instant threshold);
}