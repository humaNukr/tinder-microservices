package com.tinder.auth.scheduler;

import com.tinder.auth.entity.OutboxEvent;
import com.tinder.auth.publisher.MessageBroker;
import com.tinder.auth.repository.OutboxRepository;
import com.tinder.auth.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxRelayWorkerIT extends BaseIT {

	@Autowired
	private OutboxRelayWorker outboxRelayWorker;

	@Autowired
	private OutboxRepository outboxRepository;

	@MockitoBean
	private MessageBroker messageBroker;

	@BeforeEach
	void setUp() {
		outboxRepository.deleteAll();
		when(messageBroker.send(anyString(), anyString(), anyString()))
				.thenReturn(CompletableFuture.completedFuture(true));
	}

	@Test
	void processOutboxEvents_AllSuccessful_UpdatesDatabase() {
		outboxRepository.save(new OutboxEvent("topic-1", "{}", LocalDateTime.now()));
		outboxRepository.save(new OutboxEvent("topic-2", "{}", LocalDateTime.now()));

		outboxRelayWorker.processOutboxEvents();

		List<OutboxEvent> processedEvents = outboxRepository.findAll();

		assertAll(() -> assertEquals(2, processedEvents.size()),
				() -> assertTrue(processedEvents.stream().allMatch(OutboxEvent::getIsSent)));

		verify(messageBroker, atLeast(2)).send(anyString(), anyString(), anyString());
	}

	@Test
	void processOutboxEvents_PartialSuccess_UpdatesOnlySuccessful() {
		OutboxEvent successEvent = outboxRepository.save(new OutboxEvent("topic-success", "{}", LocalDateTime.now()));
		OutboxEvent failEvent = outboxRepository.save(new OutboxEvent("topic-fail", "{}", LocalDateTime.now()));

		when(messageBroker.send(eq("topic-success"), eq(String.valueOf(successEvent.getId())), anyString()))
				.thenReturn(CompletableFuture.completedFuture(true));

		when(messageBroker.send(eq("topic-fail"), eq(String.valueOf(failEvent.getId())), anyString()))
				.thenReturn(CompletableFuture.completedFuture(false));

		outboxRelayWorker.processOutboxEvents();

		OutboxEvent updatedSuccessEvent = outboxRepository.findById(successEvent.getId()).orElseThrow();
		OutboxEvent updatedFailEvent = outboxRepository.findById(failEvent.getId()).orElseThrow();

		assertAll(() -> assertTrue(updatedSuccessEvent.getIsSent()), () -> assertFalse(updatedFailEvent.getIsSent()));
	}

	@Test
	void processOutboxEvents_TimeoutOccurs_SavesCompletedEvents() {
		OutboxEvent fastEvent = outboxRepository.save(new OutboxEvent("topic-fast", "{}", LocalDateTime.now()));
		OutboxEvent slowEvent = outboxRepository.save(new OutboxEvent("topic-slow", "{}", LocalDateTime.now()));

		when(messageBroker.send(eq("topic-fast"), eq(String.valueOf(fastEvent.getId())), anyString()))
				.thenReturn(CompletableFuture.completedFuture(true));

		when(messageBroker.send(eq("topic-slow"), eq(String.valueOf(slowEvent.getId())), anyString()))
				.thenReturn(new CompletableFuture<>());

		outboxRelayWorker.processOutboxEvents();

		OutboxEvent updatedFastEvent = outboxRepository.findById(fastEvent.getId()).orElseThrow();
		OutboxEvent updatedSlowEvent = outboxRepository.findById(slowEvent.getId()).orElseThrow();

		assertAll(() -> assertTrue(updatedFastEvent.getIsSent()), () -> assertFalse(updatedSlowEvent.getIsSent()));
	}

	@Test
	void processOutboxEvents_EmptyDatabase_DoesNothing() {
		outboxRelayWorker.processOutboxEvents();

		assertEquals(0, outboxRepository.count());
		verify(messageBroker, times(0)).send(anyString(), anyString(), anyString());
	}

	@Test
	void processOutboxEvents_IgnoresAlreadySentEvents() {
		OutboxEvent sentEvent = new OutboxEvent("topic-sent", "{}", LocalDateTime.now());
		sentEvent.setIsSent(true);
		outboxRepository.save(sentEvent);

		OutboxEvent unsentEvent = outboxRepository.save(new OutboxEvent("topic-unsent", "{}", LocalDateTime.now()));

		when(messageBroker.send(anyString(), anyString(), anyString()))
				.thenReturn(CompletableFuture.completedFuture(true));

		outboxRelayWorker.processOutboxEvents();

		OutboxEvent updatedUnsentEvent = outboxRepository.findById(unsentEvent.getId()).orElseThrow();

		assertAll(() -> assertTrue(updatedUnsentEvent.getIsSent()),
				() -> verify(messageBroker, times(1)).send(anyString(), anyString(), anyString()));
	}
}
