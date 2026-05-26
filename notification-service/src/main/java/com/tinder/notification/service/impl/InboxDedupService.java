package com.tinder.notification.service.impl;

import com.tinder.notification.entity.InboxEvent;
import com.tinder.notification.repository.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            inboxEventRepository.saveAndFlush(new InboxEvent(eventId));
            return true;
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate event detected (eventId: {}). Skipping.", eventId);
            return false;
        }
    }
}
