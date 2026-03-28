package com.tinder.swipe.repository;

import com.tinder.swipe.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    @Query(value = """
            SELECT * FROM outbox_events 
            WHERE is_sent = false 
            ORDER BY created_at ASC 
            LIMIT :limit 
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findAndLockUnprocessedEvents(@Param("limit") int limit);
}
