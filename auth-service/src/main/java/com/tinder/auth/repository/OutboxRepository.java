package com.tinder.auth.repository;

import com.tinder.auth.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
	@Query(value = """
			SELECT * FROM outbox_events
			WHERE is_sent = false
			ORDER BY created_at ASC
			LIMIT :limit
			FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<OutboxEvent> findAndLockUnprocessedEvents(@Param("limit") int limit);

	@Modifying
	@Query("DELETE FROM OutboxEvent e WHERE e.isSent = true AND e.createdAt < :threshold")
	int deleteProcessedAndOlderThan(@Param("threshold") LocalDateTime threshold);
}
