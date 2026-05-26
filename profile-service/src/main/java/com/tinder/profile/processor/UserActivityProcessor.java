package com.tinder.profile.processor;

import com.tinder.profile.event.UserActivityEvent;
import com.tinder.profile.service.impl.InboxDedupService;
import com.tinder.profile.service.interfaces.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityProcessor {

    private final InboxDedupService inboxDedupService;
    private final ProfileService profileService;

    public void deleteProfileData(UserActivityEvent event) {
        log.info("Processing DELETE_ACCOUNT event: {}", event.eventId());

        if (!inboxDedupService.tryRegister(event.eventId())) {
            return;
        }

        profileService.deleteAccountData(event.userId());
    }
}
