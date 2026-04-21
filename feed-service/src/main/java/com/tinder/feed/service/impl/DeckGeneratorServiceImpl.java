package com.tinder.feed.service.impl;

import com.tinder.feed.adapter.ProfileServiceClient;
import com.tinder.feed.properties.FeedProperties;
import com.tinder.feed.service.interfaces.DeckGeneratorService;
import com.tinder.feed.service.interfaces.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeckGeneratorServiceImpl implements DeckGeneratorService {

    private final ProfileServiceClient profileClient;
    private final RedisService redisService;
    private final FeedProperties feedProperties;

    public void generateDeck(UUID userId) {
        List<UUID> mutableCandidates = new ArrayList<>(profileClient.fetchCandidates(userId));

        Set<UUID> swipedUsersIds = redisService.fetchSwipedUsers(userId);

        if (swipedUsersIds == null || swipedUsersIds.isEmpty()) {
            log.info("No swipes found for user {}", userId);
        } else {
            mutableCandidates.removeAll(swipedUsersIds);
        }

        List<UUID> batchCandidates = mutableCandidates.stream()
                .limit(feedProperties.deckSize())
                .toList();

        redisService.saveNewDeck(userId, batchCandidates);
    }

    @Override
    @Async
    public void generateDeckAsync(UUID userId) {
        generateDeck(userId);
    }
}