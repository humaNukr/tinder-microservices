package com.tinder.auth.service.interfaces;

import com.tinder.auth.entity.OutboxEvent;

import java.util.List;

public interface OutboxService {
    void saveEvent(String topic, Object event);

    List<OutboxEvent> fetchAndLock(int batchSize);

    void markAsFailed(List<OutboxEvent> failedEvents);
}
