package com.tinder.profile.listener;

import com.tinder.profile.event.ProfileChangedEvent;
import com.tinder.profile.service.interfaces.ProfileCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileCacheListener {

    private final ProfileCacheService cacheService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleProfileChange(ProfileChangedEvent event) {
        cacheService.cacheProfile(event.response());
    }

}
