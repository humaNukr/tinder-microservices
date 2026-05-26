package com.tinder.feed.service.impl;

import com.tinder.feed.exception.DeckGenerationInProgressException;
import com.tinder.feed.properties.FeedProperties;
import com.tinder.feed.service.interfaces.DeckGeneratorService;
import com.tinder.feed.service.interfaces.FeedStorageService;
import com.tinder.feed.service.interfaces.ProfileProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeckGeneratorServiceImpl implements DeckGeneratorService {

    private final ProfileProvider profileProvider;
    private final FeedStorageService storageService;
    private final FeedProperties feedProperties;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void generateDeck(UUID userId) {
        String lockKey = "lock:generate-deck:" + userId;
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(10));

        if (Boolean.FALSE.equals(acquired)) {
            log.warn("Deck generation already in progress for user {}", userId);
            throw new DeckGenerationInProgressException("Deck generation is already in progress.");
        }

        try {
            Set<UUID> swipedUsersIds = storageService.fetchSwipedUsers(userId);
            Set<UUID> exclude = swipedUsersIds != null ? swipedUsersIds : Set.of();

            List<UUID> batchCandidates = profileProvider.fetchCandidates(userId, exclude).stream()
                    .limit(feedProperties.deckSize())
                    .toList();

            storageService.saveNewDeck(userId, batchCandidates);
            log.info("Successfully generated new deck for user {}", userId);

        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }
}