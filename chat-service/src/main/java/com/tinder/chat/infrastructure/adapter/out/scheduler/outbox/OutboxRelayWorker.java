package com.tinder.chat.infrastructure.adapter.out.scheduler.outbox;

import com.tinder.chat.infrastructure.adapter.out.persistence.outbox.OutboxEventEntity;
import com.tinder.chat.infrastructure.adapter.out.scheduler.outbox.port.OutboxPublisherPort;
import com.tinder.chat.infrastructure.adapter.out.scheduler.outbox.port.OutboxStoragePort;
import com.tinder.chat.infrastructure.config.properties.OutboxSchedulerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayWorker {

    private final OutboxStoragePort storagePort;
    private final OutboxPublisherPort publisherPort;
    private final OutboxSchedulerProperties properties;

    @Scheduled(fixedDelayString = "${app.outbox.scheduler.fixed-delay}")
    @SchedulerLock(name = "outboxRelay", lockAtLeastFor = "2s", lockAtMostFor = "10m")
    public void processOutboxEvents() {

        List<OutboxEventEntity> events = storagePort.fetchAndLock(properties.batchSize());
        if (events.isEmpty()) return;

        List<CompletableFuture<OutboxEventEntity>> futures = events.stream()
                .map(publisherPort::send)
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(properties.batchProcessingTime(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Broker batch timeout/error", e);
        }

        List<OutboxEventEntity> failedEvents = futures.stream()
                .filter(CompletableFuture::isDone)
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        if (!failedEvents.isEmpty()) {
            storagePort.markAsFailed(failedEvents);
        }
    }
}