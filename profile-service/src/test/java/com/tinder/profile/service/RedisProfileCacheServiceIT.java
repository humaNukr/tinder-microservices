package com.tinder.profile.service;

import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.service.interfaces.ProfileCacheService;
import com.tinder.profile.util.BaseIT;
import com.tinder.profile.util.ProfileTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RedisProfileCacheService — Integration Tests")
class RedisProfileCacheServiceIT extends BaseIT {

    @Autowired
    private ProfileCacheService profileCacheService;

    private UUID userId;
    private ProfileResponse profile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profile = ProfileTestFixtures.sampleResponse(userId);
        profileCacheService.evictProfile(userId);
    }

    @Nested
    @DisplayName("cache lifecycle")
    class CacheLifecycle {

        @Test
        @DisplayName("returns empty when key is absent")
        void miss_ReturnsEmpty() {
            assertTrue(profileCacheService.getCachedProfile(userId).isEmpty());
        }

        @Test
        @DisplayName("stores and reads profile")
        void hit_ReturnsCached() {
            profileCacheService.cacheProfile(profile);

            ProfileResponse cached = profileCacheService.getCachedProfile(userId).orElseThrow();

            assertEquals(profile, cached);
        }

        @Test
        @DisplayName("evict removes cached profile")
        void evict_RemovesEntry() {
            profileCacheService.cacheProfile(profile);

            profileCacheService.evictProfile(userId);

            assertAll(
                    () -> assertTrue(profileCacheService.getCachedProfile(userId).isEmpty())
            );
        }
    }
}
