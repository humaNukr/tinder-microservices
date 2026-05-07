package com.tinder.chat.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.chat.dto.ProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileRedisServiceImpl implements ProfileRedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

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
                    log.error("Failed to parse profile json from Redis", e);
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
                log.error("Failed to serialize profile for caching: {}", profile.userId(), e);
            }
        }

        if (!cacheMap.isEmpty()) {
            redisTemplate.opsForValue().multiSet(cacheMap);
            cacheMap.keySet().forEach(key -> redisTemplate.expire(key, Duration.ofDays(7)));
        }
    }
}