package com.tinder.chat.infrastructure.adapter.out.scheduler.outbox.port;

import com.tinder.chat.infrastructure.adapter.out.persistence.outbox.OutboxEventEntity;

import java.util.concurrent.CompletableFuture;

public interface OutboxPublisherPort {
    CompletableFuture<OutboxEventEntity> send(OutboxEventEntity event);
}