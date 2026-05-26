package com.tinder.profile.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.service.interfaces.ProfileCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisProfileCacheServiceImpl implements ProfileCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<ProfileResponse> getCachedProfile(UUID userId) {
        String key = cacheKey(userId);
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, ProfileResponse.class));
        } catch (JsonProcessingException e) {
            stringRedisTemplate.delete(key);
            return Optional.empty();
        }
    }

    @Override
    public void cacheProfile(ProfileResponse profile) {
        try {
            String value = objectMapper.writeValueAsString(profile);

            stringRedisTemplate.opsForValue().set(cacheKey(profile.userId()), value, Duration.ofDays(7));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize ProfileResponse", e);
        }
    }

    @Override
    public void evictProfile(UUID userId) {
        stringRedisTemplate.delete(cacheKey(userId));
    }

    private static String cacheKey(UUID userId) {
        return "user:" + userId + ":profile";
    }
}
