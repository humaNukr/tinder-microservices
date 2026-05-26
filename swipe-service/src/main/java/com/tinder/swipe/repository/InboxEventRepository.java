package com.tinder.swipe.repository;

import com.tinder.swipe.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value =
                    """
                            INSERT INTO inbox_events (event_id, processed_at)
                            VALUES (:eventId, NOW())
                            ON CONFLICT (event_id) DO NOTHING
                            """,
            nativeQuery = true)
    int insertIfAbsent(@Param("eventId") UUID eventId);
}
