package com.tinder.feed.service.impl;

import com.tinder.feed.adapter.ProfileServiceClient;
import com.tinder.feed.dto.ProfileResponse;
import com.tinder.feed.properties.FeedProperties;
import com.tinder.feed.service.interfaces.DeckGeneratorService;
import com.tinder.feed.service.interfaces.DeckProvider;
import com.tinder.feed.service.interfaces.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeckProviderImpl implements DeckProvider {

    private final DeckGeneratorService deckGeneratorService;
    private final RedisService redisService;
    private final ProfileServiceClient profileServiceClient;
    private final FeedProperties feedProperties;

    @Override
    public List<ProfileResponse> getFeedForUser(UUID userId) {
        List<UUID> deckOfUsers = redisService.fetchDeckForUser(userId);

        if (deckOfUsers == null || deckOfUsers.isEmpty()) {
            log.warn("No deck found for userId={} in redis cache, trying to generate new deck", userId);
            deckGeneratorService.generateDeck(userId);
            deckOfUsers = redisService.fetchDeckForUser(userId);
        } else {
            Long remainingInRedis = redisService.getDeckSize(userId);
            if (remainingInRedis != null && remainingInRedis < feedProperties.refillThreshold()) {
                log.info("Deck is running low. Generating in background for user {}", userId);
                deckGeneratorService.generateDeckAsync(userId);
            }
        }

        if (deckOfUsers == null || deckOfUsers.isEmpty()) {
            log.warn("Failed to get feed for user with userId={}", userId);
            return Collections.emptyList();
        }

        return profileServiceClient.batchProfiles(deckOfUsers);
    }
}
