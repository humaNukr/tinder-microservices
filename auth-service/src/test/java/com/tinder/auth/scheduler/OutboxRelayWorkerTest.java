package com.tinder.auth.scheduler;

import com.tinder.auth.entity.OutboxEvent;
import com.tinder.auth.properties.OutboxSchedulerProperties;
import com.tinder.auth.publisher.MessageBroker;
import com.tinder.auth.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayWorkerTest {

	@Mock
	private OutboxRepository outboxRepository;

	@Mock
	private MessageBroker messageBroker;

	@Mock
	private OutboxSchedulerProperties properties;

	@InjectMocks
	private OutboxRelayWorker worker;

	@Captor
	private ArgumentCaptor<List<OutboxEvent>> savedEventsCaptor;

	@Test
    @DisplayName("Should do nothing when there are no unprocessed events")
    void processOutboxEvents_NoEvents_DoesNothing() {
        when(properties.batchSize()).thenReturn(10);
        when(outboxRepository.findAndLockUnprocessedEvents(10)).thenReturn(Collections.emptyList());

        worker.processOutboxEvents();

        verifyNoInteractions(messageBroker);
        verify(outboxRepository, never()).saveAll(any());
    }

	@Test
    @DisplayName("Should process and save all events when broker successfully sends them")
    void processOutboxEvents_AllSuccessful_UpdatesAndSavesAll() {
        when(properties.batchSize()).thenReturn(10);
        when(properties.batchProcessingTime()).thenReturn(5);

        OutboxEvent event1 = createTestEvent(1L);
        OutboxEvent event2 = createTestEvent(2L);
        when(outboxRepository.findAndLockUnprocessedEvents(10)).thenReturn(List.of(event1, event2));

        when(messageBroker.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true));

        worker.processOutboxEvents();

        verify(outboxRepository).saveAll(savedEventsCaptor.capture());
        List<OutboxEvent> savedEvents = savedEventsCaptor.getValue();

        assertAll(
                () -> assertEquals(2, savedEvents.size()),
                () -> assertTrue(savedEvents.getFirst().getIsSent()),
                () -> assertTrue(savedEvents.get(1).getIsSent())
        );
    }

	@Test
    @DisplayName("Should only save events that were successfully sent (Partial Success)")
    void processOutboxEvents_PartialSuccess_SavesOnlySuccessful() {
        when(properties.batchSize()).thenReturn(10);
        when(properties.batchProcessingTime()).thenReturn(5);

        OutboxEvent successEvent = createTestEvent(1L);
        OutboxEvent failedEvent = createTestEvent(2L);
        when(outboxRepository.findAndLockUnprocessedEvents(10)).thenReturn(List.of(successEvent, failedEvent));

        when(messageBroker.send(successEvent.getTopic(), String.valueOf(successEvent.getId()), successEvent.getPayload()))
                .thenReturn(CompletableFuture.completedFuture(true));

        when(messageBroker.send(failedEvent.getTopic(), String.valueOf(failedEvent.getId()), failedEvent.getPayload()))
                .thenReturn(CompletableFuture.completedFuture(false));

        worker.processOutboxEvents();

        verify(outboxRepository).saveAll(savedEventsCaptor.capture());
        List<OutboxEvent> savedEvents = savedEventsCaptor.getValue();

        assertAll(
                () -> assertEquals(1, savedEvents.size(), "Should only save 1 successful event"),
                () -> assertEquals(successEvent.getId(), savedEvents.getFirst().getId()),
                () -> assertTrue(savedEvents.getFirst().getIsSent())
        );
    }

	@Test
    @DisplayName("Should handle timeout gracefully and save partially completed events")
    void processOutboxEvents_Timeout_SavesCompletedBeforeTimeout() {
        when(properties.batchSize()).thenReturn(10);
        when(properties.batchProcessingTime()).thenReturn(1);

        OutboxEvent fastEvent = createTestEvent(1L);
        OutboxEvent slowEvent = createTestEvent(2L);
        when(outboxRepository.findAndLockUnprocessedEvents(10)).thenReturn(List.of(fastEvent, slowEvent));

        when(messageBroker.send(fastEvent.getTopic(), String.valueOf(fastEvent.getId()), fastEvent.getPayload()))
                .thenReturn(CompletableFuture.completedFuture(true));

        when(messageBroker.send(slowEvent.getTopic(), String.valueOf(slowEvent.getId()), slowEvent.getPayload()))
                .thenReturn(new CompletableFuture<>());

        worker.processOutboxEvents();

        verify(outboxRepository).saveAll(savedEventsCaptor.capture());
        List<OutboxEvent> savedEvents = savedEventsCaptor.getValue();

        assertAll(
                () -> assertEquals(1, savedEvents.size(), "Should save only the fast event"),
                () -> assertEquals(fastEvent.getId(), savedEvents.getFirst().getId()),
                () -> assertTrue(savedEvents.getFirst().getIsSent())
        );
    }

	private OutboxEvent createTestEvent(Long expectedId) {
		OutboxEvent event = new OutboxEvent("test-topic", "{}", LocalDateTime.now());
		ReflectionTestUtils.setField(event, "id", expectedId);
		return event;
	}
}
