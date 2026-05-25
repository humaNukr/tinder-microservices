package com.tinder.auth.scheduler;

import com.tinder.auth.entity.OutboxEvent;
import com.tinder.auth.properties.OutboxSchedulerProperties;
import com.tinder.auth.publisher.MessageBroker;
import com.tinder.auth.service.interfaces.OutboxService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayWorkerTest {

	@Mock
	private OutboxService outboxService;

	@Mock
	private MessageBroker messageBroker;

	@Mock
	private OutboxSchedulerProperties properties;

	@InjectMocks
	private OutboxRelayWorker worker;

	@Captor
	private ArgumentCaptor<List<OutboxEvent>> failedEventsCaptor;

	@BeforeEach
	void setUp() {
		lenient().when(properties.batchSize()).thenReturn(10);
		lenient().when(properties.batchProcessingTime()).thenReturn(5);
	}

	@Test
    @DisplayName("Should do nothing when there are no unprocessed events")
    void processOutboxEvents_NoEvents_DoesNothing() {
        when(outboxService.fetchAndLock(10)).thenReturn(Collections.emptyList());

        worker.processOutboxEvents();

        verifyNoInteractions(messageBroker);
        verify(outboxService, never()).markAsFailed(any());
    }

	@Test
	@DisplayName("Should not revert any events if all were sent successfully")
	void processOutboxEvents_AllSuccessful_NoEventsReverted() {
		OutboxEvent event1 = createTestEvent(1L);
		OutboxEvent event2 = createTestEvent(2L);
		when(outboxService.fetchAndLock(10)).thenReturn(List.of(event1, event2));

		when(messageBroker.send(event1)).thenReturn(CompletableFuture.completedFuture(null));
		when(messageBroker.send(event2)).thenReturn(CompletableFuture.completedFuture(null));

		worker.processOutboxEvents();

		verify(outboxService, never()).markAsFailed(anyList());
	}

	@Test
	@DisplayName("Should only revert events that failed to send (Partial Success)")
	void processOutboxEvents_PartialSuccess_RevertsOnlyFailed() {
		OutboxEvent successEvent = createTestEvent(1L);
		OutboxEvent failedEvent = createTestEvent(2L);
		when(outboxService.fetchAndLock(10)).thenReturn(List.of(successEvent, failedEvent));

		when(messageBroker.send(successEvent)).thenReturn(CompletableFuture.completedFuture(null));
		when(messageBroker.send(failedEvent)).thenReturn(CompletableFuture.completedFuture(failedEvent));

		worker.processOutboxEvents();

		verify(outboxService).markAsFailed(failedEventsCaptor.capture());
		List<OutboxEvent> revertedEvents = failedEventsCaptor.getValue();

		assertAll(() -> assertEquals(1, revertedEvents.size(), "Should only revert 1 failed event"),
				() -> assertEquals(failedEvent.getId(), revertedEvents.getFirst().getId()));
	}

	@Test
	@DisplayName("Should handle timeout gracefully and not revert pending futures")
	void processOutboxEvents_Timeout_DoesNotRevertPending() {
		OutboxEvent fastEvent = createTestEvent(1L);
		OutboxEvent slowEvent = createTestEvent(2L);
		when(outboxService.fetchAndLock(10)).thenReturn(List.of(fastEvent, slowEvent));

		when(messageBroker.send(fastEvent)).thenReturn(CompletableFuture.completedFuture(null));
		when(messageBroker.send(slowEvent)).thenReturn(new CompletableFuture<>());

		worker.processOutboxEvents();

		verify(outboxService, never()).markAsFailed(anyList());
	}

	private OutboxEvent createTestEvent(Long expectedId) {
		OutboxEvent event = new OutboxEvent("test-topic", "{}", LocalDateTime.now());
		ReflectionTestUtils.setField(event, "id", expectedId);
		return event;
	}
}
