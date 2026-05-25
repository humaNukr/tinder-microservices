package com.tinder.feed.service.impl;

import com.tinder.feed.dto.ProfileResponse;
import com.tinder.feed.properties.FeedProperties;
import com.tinder.feed.service.interfaces.DeckGeneratorService;
import com.tinder.feed.service.interfaces.DeckProvider;
import com.tinder.feed.service.interfaces.FeedStorageService;
import com.tinder.feed.service.interfaces.ProfileProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeckProviderImpl implements DeckProvider {

    private final DeckGeneratorService deckGeneratorService;
    private final AsyncDeckGenerator asyncDeckGenerator;
    private final FeedStorageService storageService;
    private final ProfileProvider profileProvider;
    private final FeedProperties feedProperties;

    @Override
    public List<ProfileResponse> getFeedForUser(UUID userId) {
        List<UUID> deckOfUsers = storageService.fetchDeckForUser(userId, feedProperties.pageSize());

        if (deckOfUsers == null || deckOfUsers.isEmpty()) {
            log.warn("No deck found for userId={} in cache, generating synchronously", userId);
            deckGeneratorService.generateDeck(userId);
            deckOfUsers = storageService.fetchDeckForUser(userId, feedProperties.pageSize());
        } else {
            Long remainingInRedis = storageService.getDeckSize(userId);
            if (remainingInRedis != null && remainingInRedis < feedProperties.refillThreshold()) {
                log.info("Deck running low for user {}. Triggering background generation.", userId);
                asyncDeckGenerator.generateDeckAsync(userId);
            }
        }

        if (deckOfUsers == null || deckOfUsers.isEmpty()) {
            log.warn("Feed exhausted for user {}", userId);
            return Collections.emptyList();
        }

        List<ProfileResponse> cachedProfiles = storageService.getCachedProfiles(deckOfUsers);

        if (cachedProfiles == null || cachedProfiles.isEmpty()) {
            return profileProvider.batchProfiles(deckOfUsers);
        }

        if (cachedProfiles.size() != deckOfUsers.size()) {
            List<UUID> cachedIds = cachedProfiles.stream().map(ProfileResponse::userId).toList();
            List<UUID> missingIds = new ArrayList<>(deckOfUsers);
            missingIds.removeAll(cachedIds);

            List<ProfileResponse> restOfProfiles = profileProvider.batchProfiles(missingIds);
            storageService.cacheProfiles(restOfProfiles);
            cachedProfiles.addAll(restOfProfiles);
        }

        return cachedProfiles;
    }
}