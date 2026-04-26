package com.tinder.auth.scheduler;

import com.tinder.auth.entity.OutboxEvent;
import com.tinder.auth.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
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
	private final KafkaTemplate<String, Object> kafkaTemplate;
	@Value("${outbox.scheduler.batch-size}")
	private int batchSize;
	@Value("${outbox.scheduler.batch-processing-time}")
	private int batchProcessingTime;

	@Scheduled(fixedDelayString = "${outbox.scheduler.fixed-delay}")
	@Transactional
	public void processOutboxEvents() {
		List<OutboxEvent> events = outboxRepository.findAndLockUnprocessedEvents(batchSize);
		if (events.isEmpty())
			return;

		List<CompletableFuture<OutboxEvent>> futures = events.stream()
				.map(event -> kafkaTemplate.send(event.getTopic(), String.valueOf(event.getId()), event.getPayload())
						.thenApply(sendResult -> event).exceptionally(ex -> {
							log.error("Failed to send event {}", event.getId(), ex);
							return null;
						}))
				.toList();

		try {
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(batchProcessingTime,
					TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			log.warn("Kafka batch processing timed out. Partially completed events will be saved.");
		} catch (Exception e) {
			log.error("Unexpected error while waiting for Kafka batch.", e);
		}

		List<OutboxEvent> successfulEvents = futures.stream().filter(CompletableFuture::isDone)
				.map(CompletableFuture::join).filter(Objects::nonNull).peek(event -> event.setIsSent(true)).toList();

		if (!successfulEvents.isEmpty()) {
			outboxRepository.saveAll(successfulEvents);
			log.debug("Successfully processed and saved {}/{} events", successfulEvents.size(), events.size());
		}
	}
}
