package com.tinder.auth.scheduler;

import com.tinder.auth.entity.OutboxEvent;
import com.tinder.auth.properties.OutboxSchedulerProperties;
import com.tinder.auth.publisher.MessageBroker;
import com.tinder.auth.service.interfaces.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayWorker {

	private final OutboxService outboxService;
	private final MessageBroker messageBroker;
	private final OutboxSchedulerProperties properties;

	@Scheduled(fixedDelayString = "${app.outbox.scheduler.fixed-delay}")
	public void processOutboxEvents() {

		List<OutboxEvent> events = outboxService.fetchAndLock(properties.batchSize());
		if (events.isEmpty()) {
			return;
		}

		List<CompletableFuture<OutboxEvent>> futures = events.stream().map(messageBroker::send).toList();

		try {
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(properties.batchProcessingTime(),
					TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			log.warn("Message broker batch timed out after {}s", properties.batchProcessingTime());
		} catch (Exception e) {
			log.error("Unexpected error waiting for message broker batch", e);
		}

		List<OutboxEvent> failedEvents = futures.stream().filter(CompletableFuture::isDone).map(CompletableFuture::join)
				.filter(Objects::nonNull).toList();

		if (!failedEvents.isEmpty()) {
			outboxService.markAsFailed(failedEvents);
			log.debug("Reverted {} failed events back to pending", failedEvents.size());
		}
	}
}
