package com.tinder.notification.processor;

import com.tinder.notification.event.UserActivityEvent;
import com.tinder.notification.service.AccountDeletionService;
import com.tinder.notification.service.impl.InboxDedupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionProcessor {

    private final InboxDedupService inboxDedupService;
    private final AccountDeletionService accountDeletionService;

    public void process(UserActivityEvent event) {
        log.info("Processing DELETE_ACCOUNT for user {} (eventId: {})", event.userId(), event.eventId());

        if (!inboxDedupService.tryRegister(event.eventId())) {
            return;
        }

        accountDeletionService.deleteUserData(event.userId());
    }
}
