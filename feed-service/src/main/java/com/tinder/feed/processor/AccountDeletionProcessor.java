package com.tinder.feed.processor;

import com.tinder.feed.event.UserActivityEvent;
import com.tinder.feed.service.interfaces.FeedStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionProcessor {

    private final FeedStorageService feedStorageService;

    public void process(UserActivityEvent event) {
        log.info("Processing DELETE_ACCOUNT for user {} (eventId: {})", event.userId(), event.eventId());
        feedStorageService.deleteUserFeedData(event.userId());
    }
}
