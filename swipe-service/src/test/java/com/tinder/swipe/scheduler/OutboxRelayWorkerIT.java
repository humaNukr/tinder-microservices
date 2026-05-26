package com.tinder.swipe.scheduler;

import com.tinder.swipe.entity.OutboxEvent;
import com.tinder.swipe.repository.OutboxRepository;
import com.tinder.swipe.util.BaseIT;
import com.tinder.swipe.util.SwipeTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OutboxRelayWorker — Integration Tests")
class OutboxRelayWorkerIT extends BaseIT {

    @Autowired
    private OutboxRelayWorker outboxRelayWorker;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    @DisplayName("processOutboxEvents() marks all events as sent when Kafka succeeds")
    void processOutboxEvents_AllSuccessful_UpdatesDatabase() {
        outboxRepository.save(new OutboxEvent(
                "swipe-events-test",
                SwipeTestFixtures.swipeCreatedEvent(
                        SwipeTestFixtures.USER_ONE, SwipeTestFixtures.USER_TWO, true),
                LocalDateTime.now()));
        outboxRepository.save(new OutboxEvent(
                "match-events-test",
                SwipeTestFixtures.matchEvent(
                        java.util.UUID.randomUUID(), SwipeTestFixtures.USER_ONE, SwipeTestFixtures.USER_TWO),
                LocalDateTime.now()));

        outboxRelayWorker.processOutboxEvents();

        await().atMost(20, SECONDS).untilAsserted(() -> {
            var processed = outboxRepository.findAll();
            assertAll(
                    () -> assertEquals(2, processed.size()),
                    () -> assertTrue(processed.stream().allMatch(OutboxEvent::isSent)));
        });
    }

    @Test
    @DisplayName("processOutboxEvents() does nothing when outbox is empty")
    void processOutboxEvents_EmptyDatabase_DoesNothing() {
        outboxRelayWorker.processOutboxEvents();

        assertEquals(0, outboxRepository.count());
    }
}
