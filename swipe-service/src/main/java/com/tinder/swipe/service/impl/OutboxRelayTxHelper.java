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
        return outboxRepository.findAndLockUnprocessedEvents(limit);
    }

    @Transactional
    public void markAsSent(List<OutboxEvent> events) {
        events.forEach(event -> event.setSent(true));
        outboxRepository.saveAll(events);
    }
}
