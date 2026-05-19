package com.tinder.chat.infrastructure.adapter.out.scheduler;

import com.tinder.chat.infrastructure.adapter.out.persistence.outbox.OutboxEventEntity;
import com.tinder.chat.infrastructure.adapter.out.scheduler.outbox.OutboxRelayWorker;
import com.tinder.chat.infrastructure.adapter.out.scheduler.outbox.port.OutboxPublisherPort;
import com.tinder.chat.infrastructure.adapter.out.scheduler.outbox.port.OutboxStoragePort;
import com.tinder.chat.infrastructure.config.properties.OutboxSchedulerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayWorkerTest {

    @Mock
    private OutboxStoragePort storagePort;
    @Mock
    private OutboxPublisherPort publisherPort;
    @Mock
    private OutboxSchedulerProperties properties;

    @InjectMocks
    private OutboxRelayWorker worker;

    @Captor
    private ArgumentCaptor<List<OutboxEventEntity>> failedEventsCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(properties.batchSize()).thenReturn(100);
        lenient().when(properties.batchProcessingTime()).thenReturn(5);
    }

    @Test
    void processOutboxEvents_NoEvents_DoesNothing() {
        when(storagePort.fetchAndLock(100)).thenReturn(Collections.emptyList());

        worker.processOutboxEvents();

        verifyNoInteractions(publisherPort);
        verify(storagePort, never()).markAsFailed(anyList());
    }

    @Test
    void processOutboxEvents_AllSuccessful_NoEventsReverted() {
        OutboxEventEntity event1 = createEvent(1L, "topic1", "payload1");
        OutboxEventEntity event2 = createEvent(2L, "topic1", "payload2");

        when(storagePort.fetchAndLock(100)).thenReturn(List.of(event1, event2));

        when(publisherPort.send(event1)).thenReturn(CompletableFuture.completedFuture(null));
        when(publisherPort.send(event2)).thenReturn(CompletableFuture.completedFuture(null));

        worker.processOutboxEvents();

        verify(storagePort, never()).markAsFailed(anyList());
    }

    @Test
    void processOutboxEvents_PartialSuccess_RevertsOnlyFailedEvents() {
        OutboxEventEntity successEvent = createEvent(1L, "topic1", "payload1");
        OutboxEventEntity failedEvent = createEvent(2L, "topic1", "payload2");

        when(storagePort.fetchAndLock(100)).thenReturn(List.of(successEvent, failedEvent));

        when(publisherPort.send(successEvent)).thenReturn(CompletableFuture.completedFuture(null));
        when(publisherPort.send(failedEvent)).thenReturn(CompletableFuture.completedFuture(failedEvent));

        worker.processOutboxEvents();

        verify(storagePort).markAsFailed(failedEventsCaptor.capture());

        List<OutboxEventEntity> revertedEvents = failedEventsCaptor.getValue();

        assertAll(
                () -> assertEquals(1, revertedEvents.size(), "Only one event should be reverted"),
                () -> assertEquals(2L, revertedEvents.get(0).getId(), "The failed event ID should match")
        );
    }

    private OutboxEventEntity createEvent(Long id, String topic, String payload) {
        OutboxEventEntity event = new OutboxEventEntity(topic, payload, LocalDateTime.now());
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}