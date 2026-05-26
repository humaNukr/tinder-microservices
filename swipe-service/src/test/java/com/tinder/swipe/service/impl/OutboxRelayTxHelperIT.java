package com.tinder.swipe.service.impl;

import com.tinder.swipe.entity.OutboxEvent;
import com.tinder.swipe.repository.OutboxRepository;
import com.tinder.swipe.util.BaseIT;
import com.tinder.swipe.util.SwipeTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OutboxRelayTxHelper — Integration Tests")
class OutboxRelayTxHelperIT extends BaseIT {

    @Autowired
    private OutboxRelayTxHelper outboxRelayTxHelper;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    @DisplayName("findAndLockUnprocessed() returns unsent events")
    void findAndLockUnprocessed_UnsentEvents_ReturnsRows() {
        outboxRepository.save(new OutboxEvent(
                "swipe-events-test",
                SwipeTestFixtures.swipeCreatedEvent(
                        SwipeTestFixtures.USER_ONE, SwipeTestFixtures.USER_TWO, true),
                LocalDateTime.now()));

        var locked = outboxRelayTxHelper.findAndLockUnprocessed(10);

        assertEquals(1, locked.size());
        assertTrue(locked.getFirst().isSent());
    }

    @Test
    @DisplayName("markAsFailed() reverts sent flag so event can be retried")
    void markAsFailed_FailedEvents_RevertsToPending() {
        OutboxEvent event = outboxRepository.save(new OutboxEvent(
                "swipe-events-test",
                SwipeTestFixtures.swipeCreatedEvent(
                        SwipeTestFixtures.USER_ONE, SwipeTestFixtures.USER_TWO, true),
                LocalDateTime.now()));
        event.setSent(true);
        outboxRepository.save(event);

        outboxRelayTxHelper.markAsFailed(java.util.List.of(event));

        assertFalse(outboxRepository.findById(event.getId()).orElseThrow().isSent());
    }

    @Test
    @DisplayName("KafkaTemplate can publish payload loaded from the outbox table")
    void kafkaTemplate_DbPayload_PublishesSuccessfully() throws Exception {
        OutboxEvent event = outboxRepository.save(new OutboxEvent(
                "swipe-events-test",
                SwipeTestFixtures.swipeCreatedEvent(
                        SwipeTestFixtures.USER_ONE, SwipeTestFixtures.USER_TWO, true),
                LocalDateTime.now()));

        Object payload = outboxRepository.findById(event.getId()).orElseThrow().getPayload();

        kafkaTemplate
                .send("swipe-events-test", String.valueOf(event.getId()), payload)
                .get(15, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("KafkaTemplate can publish swipe events to the test cluster")
    void kafkaTemplate_SwipeEvent_PublishesSuccessfully() throws Exception {
        var future = kafkaTemplate.send(
                "swipe-events-test",
                "1",
                SwipeTestFixtures.swipeCreatedEvent(
                        SwipeTestFixtures.USER_ONE, SwipeTestFixtures.USER_TWO, true));

        future.get(15, TimeUnit.SECONDS);
    }
}
