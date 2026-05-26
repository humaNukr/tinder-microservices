package com.tinder.chat.application.service.account;

import com.tinder.chat.infrastructure.adapter.out.persistence.inbox.InboxEventEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.inbox.InboxEventJpaRepository;
import com.tinder.chat.shared.dto.event.UserActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionProcessor {

    private final InboxEventJpaRepository inboxEventRepository;
    private final AccountDeletionService accountDeletionService;

    @Transactional
    public void process(UserActivityEvent event) {
        log.info("Processing DELETE_ACCOUNT for user {} (eventId: {})", event.userId(), event.eventId());

        if (inboxEventRepository.existsByEventId(event.eventId())) {
            log.debug("DELETE_ACCOUNT event {} already processed. Skipping.", event.eventId());
            return;
        }

        inboxEventRepository.save(new InboxEventEntity(event.eventId()));
        accountDeletionService.deleteUserData(event.userId());
    }
}
