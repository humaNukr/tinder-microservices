package com.tinder.feed.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.feed.dto.ProfileResponse;
import com.tinder.feed.properties.FeedProperties;
import com.tinder.feed.service.interfaces.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisServiceImpl implements RedisService {

    private final StringRedisTemplate redisTemplate;
    private final FeedProperties feedProperties;
    private final ObjectMapper objectMapper;

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

    @Override
    public List<ProfileResponse> getCachedProfiles(List<UUID> usersIds) {
        if (usersIds == null || usersIds.isEmpty()) return new ArrayList<>();

        List<String> keys = usersIds.stream()
                .map(id -> "user:" + id + ":profile")
                .toList();

        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        List<ProfileResponse> profiles = new ArrayList<>();
        if (values == null) return profiles;

        for (String value : values) {
            if (value != null) {
                try {
                    profiles.add(objectMapper.readValue(value, ProfileResponse.class));
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse profile json", e);
                }
            }
        }
        return profiles;
    }

    @Override
    public void cacheProfiles(List<ProfileResponse> profiles) {
        if (profiles == null || profiles.isEmpty()) return;

        Map<String, String> cacheMap = new HashMap<>();
        for (ProfileResponse profile : profiles) {
            try {
                String key = "user:" + profile.userId() + ":profile";
                cacheMap.put(key, objectMapper.writeValueAsString(profile));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize profile for lazy caching: {}", profile.userId(), e);
            }
        }

        redisTemplate.opsForValue().multiSet(cacheMap);

        cacheMap.keySet().forEach(key -> redisTemplate.expire(key, Duration.ofDays(7))
        );
    }
}