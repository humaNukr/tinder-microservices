package com.tinder.profile.service.impl;

import com.tinder.profile.domain.InboxEvent;
import com.tinder.profile.repository.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboxDedupService {

    private final InboxEventRepository inboxEventRepository;

    /**
     * Atomically claims an event id before processing (MongoDB unique index on eventId).
     */
    public boolean tryRegister(UUID eventId) {
        try {
            inboxEventRepository.save(new InboxEvent(eventId));
            return true;
        } catch (DuplicateKeyException e) {
            log.warn("Duplicate event detected (eventId: {}). Skipping.", eventId);
            return false;
        }
    }
}
