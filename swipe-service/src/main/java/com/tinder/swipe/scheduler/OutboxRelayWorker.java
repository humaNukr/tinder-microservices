package com.tinder.swipe.scheduler;

import com.tinder.swipe.entity.OutboxEvent;
import com.tinder.swipe.properties.OutboxSchedulerProperties;
import com.tinder.swipe.service.impl.OutboxRelayTxHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.kafka.core.KafkaTemplate;
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

    private final OutboxRelayTxHelper outboxRelayTxHelper;
    private final OutboxSchedulerProperties outboxSchedulerProperties;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.scheduler.fixed-delay}")
    @SchedulerLock(name = "outboxRelay", lockAtLeastFor = "2s", lockAtMostFor = "10m")
    public void processOutboxEvents() {
        List<OutboxEvent> events = outboxRelayTxHelper.findAndLockUnprocessed(
                outboxSchedulerProperties.batchSize());
        if (events.isEmpty()) {
            return;
        }

        List<CompletableFuture<OutboxEvent>> futures = events.stream()
                .map(event -> kafkaTemplate.send(
                                event.getTopic(),
                                String.valueOf(event.getId()),
                                event.getPayload())
                        .thenApply(sendResult -> event)
                        .exceptionally(ex -> {
                            log.error("Failed to send event {}", event.getId(), ex);
                            return null;
                        }))
                .toList();

        int batchProcessingTime = outboxSchedulerProperties.batchProcessingTime();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(batchProcessingTime, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Kafka batch processing timed out. Partially completed events will be saved.");
        } catch (Exception e) {
            log.error("Unexpected error while waiting for Kafka batch.", e);
        }

        List<OutboxEvent> successfulEvents = futures.stream()
                .filter(CompletableFuture::isDone)
                .filter(future -> !future.isCompletedExceptionally())
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        if (!successfulEvents.isEmpty()) {
            outboxRelayTxHelper.markAsSent(successfulEvents);
            log.debug("Successfully processed and saved {}/{} events", successfulEvents.size(), events.size());
        }
    }
}
