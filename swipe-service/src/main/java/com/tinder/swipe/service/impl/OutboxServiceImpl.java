package com.tinder.swipe.service.impl;

import com.tinder.swipe.entity.OutboxEvent;
import com.tinder.swipe.repository.OutboxRepository;
import com.tinder.swipe.service.interfaces.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {
    private final OutboxRepository outboxRepository;

    @Override
    @Transactional
    public void saveEvent(String topic, Object event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        OutboxEvent outboxEvent = new OutboxEvent(topic, event, LocalDateTime.now());
        outboxRepository.save(outboxEvent);
    }
}
