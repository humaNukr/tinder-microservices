package com.tinder.swipe.service.impl;

import com.tinder.swipe.entity.OutboxEvent;
import com.tinder.swipe.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxRelayTxHelper {

    private final OutboxRepository outboxRepository;

    @Transactional
    public List<OutboxEvent> findAndLockUnprocessed(int limit) {
        List<OutboxEvent> events = outboxRepository.findAndLockUnprocessedEvents(limit);
        if (events.isEmpty()) {
            return events;
        }
        events.forEach(event -> event.setSent(true));
        return outboxRepository.saveAll(events);
    }

    @Transactional
    public void markAsFailed(List<OutboxEvent> failedEvents) {
        if (failedEvents.isEmpty()) {
            return;
        }
        failedEvents.forEach(event -> event.setSent(false));
        outboxRepository.saveAll(failedEvents);
    }
}
