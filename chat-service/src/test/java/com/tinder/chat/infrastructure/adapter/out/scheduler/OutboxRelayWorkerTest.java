package com.tinder.chat.infrastructure.adapter.out.scheduler;

import com.tinder.chat.infrastructure.adapter.out.persistence.outbox.OutboxEventEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.outbox.OutboxJpaRepository;
import com.tinder.chat.infrastructure.adapter.out.scheduler.outbox.OutboxRelayWorker;
import com.tinder.chat.infrastructure.config.properties.OutboxSchedulerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayWorkerTest {

    @Mock
    private OutboxJpaRepository outboxRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private OutboxSchedulerProperties properties;

    @InjectMocks
    private OutboxRelayWorker worker;

    @Captor
    private ArgumentCaptor<List<OutboxEventEntity>> savedEventsCaptor;

    @BeforeEach
    void setUp() {
        when(properties.batchSize()).thenReturn(100);
    }

    @Test
    void processOutboxEvents_NoEvents_DoesNothing() {
        when(outboxRepository.findAndLockUnprocessedEvents(100)).thenReturn(Collections.emptyList());

        worker.processOutboxEvents();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        verify(outboxRepository, never()).saveAll(anyList());
    }

    @Test
    void processOutboxEvents_AllSuccessful_UpdatesAndSavesAll() {
        when(properties.batchProcessingTime()).thenReturn(5);

        OutboxEventEntity event1 = createEvent(1L, "topic1", "payload1");
        OutboxEventEntity event2 = createEvent(2L, "topic1", "payload2");

        when(outboxRepository.findAndLockUnprocessedEvents(100)).thenReturn(List.of(event1, event2));

        CompletableFuture<SendResult<String, String>> future1 = CompletableFuture.completedFuture(mock(SendResult.class));
        CompletableFuture<SendResult<String, String>> future2 = CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send("topic1", "1", "payload1")).thenReturn(future1);
        when(kafkaTemplate.send("topic1", "2", "payload2")).thenReturn(future2);

        worker.processOutboxEvents();

        verify(outboxRepository).saveAll(savedEventsCaptor.capture());
        List<OutboxEventEntity> savedEvents = savedEventsCaptor.getValue();

        assertEquals(2, savedEvents.size());
        assertTrue(savedEvents.get(0).getIsSent());
        assertTrue(savedEvents.get(1).getIsSent());
    }

    @Test
    void processOutboxEvents_PartialSuccess_SavesOnlySuccessfulEvents() {
        when(properties.batchProcessingTime()).thenReturn(5);

        OutboxEventEntity successEvent = createEvent(1L, "topic1", "payload1");
        OutboxEventEntity failedEvent = createEvent(2L, "topic1", "payload2");

        when(outboxRepository.findAndLockUnprocessedEvents(100)).thenReturn(List.of(successEvent, failedEvent));

        CompletableFuture<SendResult<String, String>> successFuture = CompletableFuture.completedFuture(mock(SendResult.class));
        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka timeout"));

        when(kafkaTemplate.send("topic1", "1", "payload1")).thenReturn(successFuture);
        when(kafkaTemplate.send("topic1", "2", "payload2")).thenReturn(failedFuture);

        worker.processOutboxEvents();

        verify(outboxRepository).saveAll(savedEventsCaptor.capture());
        List<OutboxEventEntity> savedEvents = savedEventsCaptor.getValue();

        assertEquals(1, savedEvents.size());
        assertEquals(1L, savedEvents.get(0).getId());
        assertTrue(savedEvents.get(0).getIsSent());
    }

    private OutboxEventEntity createEvent(Long id, String topic, String payload) {
        OutboxEventEntity event = new OutboxEventEntity(topic, payload, LocalDateTime.now());
        ReflectionTestUtils.setField(event, "id", id);

        return event;
    }
}