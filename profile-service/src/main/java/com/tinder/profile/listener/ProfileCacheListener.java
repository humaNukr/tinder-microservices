package com.tinder.profile.listener;

import com.tinder.profile.event.ProfileChangedEvent;
import com.tinder.profile.service.interfaces.ProfileCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileCacheListener {

    private final ProfileCacheService cacheService;

    @EventListener
    @Async
    public void handleProfileChange(ProfileChangedEvent event) {
        cacheService.cacheProfile(event.response());
    }
}
