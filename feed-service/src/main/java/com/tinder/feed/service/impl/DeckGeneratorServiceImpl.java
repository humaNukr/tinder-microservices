package com.tinder.feed.service.impl;

import com.tinder.feed.adapter.ProfileClient;
import com.tinder.feed.service.interfaces.DeckGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeckGeneratorServiceImpl implements DeckGeneratorService {

    private final ProfileClient profileClient;
    private final RedisTemplate<String, UUID> redisForHistory;
    private final RedisTemplate<String, List<UUID>> redisForDecks;

    public void generateDeck(UUID userId) {
        List<UUID> candidates = profileClient.fetchCandidates(userId);

        Set<UUID> swipedUsersIds = redisForHistory.opsForSet().members("user:" + userId + ":history");

        if (swipedUsersIds == null || swipedUsersIds.isEmpty()) {
            log.info("No candidates found for user {}", userId);
        } else {
            candidates.removeAll(swipedUsersIds);
        }

        List<UUID> batchCandidates = candidates.subList(0, Math.min(candidates.size(), 50));

        String deckKey = "user:" + userId + ":deck";
        redisForDecks.delete(deckKey);
        redisForDecks.opsForList().rightPush(deckKey, batchCandidates);
    }
}