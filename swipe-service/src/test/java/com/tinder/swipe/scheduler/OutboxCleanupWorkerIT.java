package com.tinder.swipe.scheduler;

import com.tinder.swipe.entity.OutboxEvent;
import com.tinder.swipe.repository.OutboxRepository;
import com.tinder.swipe.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("OutboxCleanupWorker — Integration Tests")
class OutboxCleanupWorkerIT extends BaseIT {

    @Autowired
    private OutboxCleanupWorker outboxCleanupWorker;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    @DisplayName("cleanupOutbox() deletes old processed events")
    void cleanupOutbox_OldProcessedEvents_DeletesRows() {
        OutboxEvent oldSent = outboxRepository.save(new OutboxEvent("topic", "{}", LocalDateTime.now()));
        oldSent.setSent(true);
        outboxRepository.save(oldSent);

        jdbcTemplate.update(
                "UPDATE outbox_events SET created_at = ? WHERE id = ?",
                LocalDateTime.now().minusDays(8),
                oldSent.getId());

        OutboxEvent recentSent = new OutboxEvent("topic", "{}", LocalDateTime.now());
        recentSent.setSent(true);
        outboxRepository.save(recentSent);

        outboxCleanupWorker.cleanupOutbox();

        assertEquals(1, outboxRepository.count());
    }
}
