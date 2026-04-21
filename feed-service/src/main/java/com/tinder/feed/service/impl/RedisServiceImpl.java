package com.tinder.feed.service.impl;

import com.tinder.feed.properties.FeedProperties;
import com.tinder.feed.service.interfaces.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final StringRedisTemplate redisTemplate;
    private final FeedProperties feedProperties;

    @Override
    public Set<UUID> fetchSwipedUsers(UUID userId) {
        Set<String> members = redisTemplate.opsForSet().members("user:" + userId + ":history");
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        return members.stream().map(UUID::fromString).collect(Collectors.toSet());
    }

    @Override
    public void saveNewDeck(UUID userId, List<UUID> batchCandidates) {
        if (batchCandidates == null || batchCandidates.isEmpty()) {
            return;
        }
        String deckKey = "user:" + userId + ":deck";
        List<String> stringIds = batchCandidates.stream().map(UUID::toString).toList();

        redisTemplate.delete(deckKey);
        redisTemplate.opsForList().rightPushAll(deckKey, stringIds);
    }

    @Override
    public List<UUID> fetchDeckForUser(UUID userId) {
        String deckKey = "user:" + userId + ":deck";
        List<String> poppedIds = redisTemplate.opsForList().leftPop(deckKey, feedProperties.pageSize());

        if (poppedIds == null || poppedIds.isEmpty()) {
            return Collections.emptyList();
        }
        return poppedIds.stream().map(UUID::fromString).toList();
    }

    @Override
    public Long getDeckSize(UUID userId) {
        String deckKey = "user:" + userId + ":deck";
        return redisTemplate.opsForList().size(deckKey);
    }

    @Override
    public void addSwipedUserToHistory(UUID swiperId, UUID swipedId) {
        String historyKey = "user:" + swiperId + ":history";
        redisTemplate.opsForSet().add(historyKey, swipedId.toString());
        redisTemplate.expire(historyKey, Duration.ofDays(30));
    }

    @Override
    public void deleteDeck(UUID userId) {
        String deckKey = "user:" + userId + ":deck";
        redisTemplate.delete(deckKey);
    }
}