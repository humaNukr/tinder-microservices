package com.tinder.swipe.service.impl;

import com.tinder.swipe.repository.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboxDedupService {

    private final InboxEventRepository inboxEventRepository;

    @Transactional
    public boolean tryRegister(UUID eventId) {
        int inserted = inboxEventRepository.insertIfAbsent(eventId);
        if (inserted == 0) {
            log.warn("Duplicate event detected (eventId: {}). Skipping.", eventId);
            return false;
        }
        return true;
    }
}
