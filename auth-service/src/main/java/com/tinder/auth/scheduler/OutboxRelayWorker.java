package com.tinder.auth.scheduler;

import com.tinder.auth.entity.OutboxEvent;
import com.tinder.auth.properties.OutboxSchedulerProperties;
import com.tinder.auth.publisher.MessageBroker;
import com.tinder.auth.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayWorker {

	private final OutboxRepository outboxRepository;
	private final MessageBroker messageBroker;
	private final OutboxSchedulerProperties outboxSchedulerProperties;

	@Scheduled(fixedDelayString = "${app.outbox.scheduler.fixed-delay}")
	@Transactional
	public void processOutboxEvents() {
		List<OutboxEvent> events = outboxRepository.findAndLockUnprocessedEvents(outboxSchedulerProperties.batchSize());
		if (events.isEmpty()) {
			return;
		}

		List<CompletableFuture<OutboxEvent>> futures = events.stream()
				.map(event -> messageBroker.send(event.getTopic(), String.valueOf(event.getId()), event.getPayload())
						.thenApply(isSuccess -> isSuccess ? event : null))
				.toList();

		try {
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
					.get(outboxSchedulerProperties.batchProcessingTime(), TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			log.warn("Message broker batch processing timed out. Partially completed events will be saved.");
		} catch (Exception e) {
			log.error("Unexpected error while waiting for message broker batch.", e);
		}

		List<OutboxEvent> successfulEvents = futures.stream().filter(CompletableFuture::isDone)
				.map(CompletableFuture::join).filter(Objects::nonNull).peek(event -> event.setIsSent(true)).toList();

		if (!successfulEvents.isEmpty()) {
			outboxRepository.saveAll(successfulEvents);
			log.debug("Successfully processed and saved {}/{} events", successfulEvents.size(), events.size());
		}
	}
}
