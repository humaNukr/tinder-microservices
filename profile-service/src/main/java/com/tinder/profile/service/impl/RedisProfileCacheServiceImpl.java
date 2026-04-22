package com.tinder.profile.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.service.interfaces.ProfileCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisProfileCacheServiceImpl implements ProfileCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void cacheProfile(ProfileResponse profile) {
        try {
            String key = "user:" + profile.userId() + ":profile";
            String value = objectMapper.writeValueAsString(profile);

            stringRedisTemplate.opsForValue().set(key, value, Duration.ofDays(7));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize ProfileResponse", e);
        }
    }
}
