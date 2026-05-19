package com.tinder.chat.infrastructure.adapter.out.persistence.outbox;

import com.tinder.chat.infrastructure.adapter.out.scheduler.outbox.port.OutboxStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements OutboxStoragePort {

    private final OutboxJpaRepository outboxRepository;

    @Transactional
    public List<OutboxEventEntity> fetchAndLock(int batchSize) {
        List<OutboxEventEntity> events = outboxRepository.findAndLockUnprocessedEvents(batchSize);
        if (events.isEmpty()) {
            return events;
        }

        events.forEach(e -> e.setIsSent(true));
        return outboxRepository.saveAll(events);
    }

    @Transactional
    public void markAsFailed(List<OutboxEventEntity> failedEvents) {
        if (failedEvents.isEmpty()) {
            return;
        }

        failedEvents.forEach(e -> e.setIsSent(false));
        outboxRepository.saveAll(failedEvents);
    }
}