package com.tinder.chat.infrastructure.adapter.out.kafka;

import com.tinder.chat.infrastructure.adapter.out.persistence.outbox.OutboxEventEntity;
import com.tinder.chat.infrastructure.adapter.out.scheduler.outbox.port.OutboxPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxKafkaAdapter implements OutboxPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public CompletableFuture<OutboxEventEntity> send(OutboxEventEntity event) {
        return kafkaTemplate
                .send(event.getTopic(), String.valueOf(event.getId()), event.getPayload())
                .thenApply(sendResult -> (OutboxEventEntity) null)
                .exceptionally(ex -> {
                    log.error("Failed to send OutboxEvent {}: {}", event.getId(), ex.getMessage());
                    return event;
                });
    }
}