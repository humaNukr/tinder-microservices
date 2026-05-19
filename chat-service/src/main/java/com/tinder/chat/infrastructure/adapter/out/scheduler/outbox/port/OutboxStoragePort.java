package com.tinder.chat.infrastructure.adapter.out.scheduler.outbox.port;

import com.tinder.chat.infrastructure.adapter.out.persistence.outbox.OutboxEventEntity;

import java.util.List;

public interface OutboxStoragePort {
    List<OutboxEventEntity> fetchAndLock(int batchSize);

    void markAsFailed(List<OutboxEventEntity> failedEvents);
}