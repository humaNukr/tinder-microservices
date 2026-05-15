package com.tinder.chat.infrastructure.adapter.out.profile;

import com.tinder.chat.shared.dto.external.ProfileResponse;
import com.tinder.chat.util.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisProfileCacheAdapterIT extends IntegrationTestBase {

    @Autowired
    private RedisProfileCacheAdapter adapter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void cacheAndGetProfiles_FullCycle_WorksCorrectly() {
        UUID userId = UUID.randomUUID();
        ProfileResponse profile = new ProfileResponse(
                userId, "Alex", 22, "M", "Developer",
                List.of("Java", "Testing"), List.of("img1.png")
        );

        adapter.cacheProfiles(List.of(profile));

        String redisKey = "user:" + userId + ":profile";
        assertTrue(redisTemplate.hasKey(redisKey));

        List<ProfileResponse> cached = adapter.getCachedProfiles(List.of(userId));

        assertEquals(1, cached.size());
        ProfileResponse result = cached.get(0);
        assertEquals(userId, result.userId());
        assertEquals("Alex", result.name());
        assertEquals(2, result.interests().size());
        assertTrue(result.interests().contains("Java"));
    }

    @Test
    void getCachedProfiles_PartialMiss_ReturnsOnlyExisting() {
        UUID idInCache = UUID.randomUUID();
        UUID idMissing = UUID.randomUUID();

        ProfileResponse profile = new ProfileResponse(
                idInCache, "Cached", 20, "F", "Bio", List.of(), List.of());
        adapter.cacheProfiles(List.of(profile));

        List<ProfileResponse> results = adapter.getCachedProfiles(List.of(idInCache, idMissing));

        assertEquals(1, results.size());
        assertEquals(idInCache, results.get(0).userId());
    }
}