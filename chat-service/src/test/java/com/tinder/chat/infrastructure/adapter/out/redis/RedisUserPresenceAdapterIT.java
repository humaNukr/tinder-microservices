package com.tinder.chat.infrastructure.adapter.out.redis;

import com.tinder.chat.infrastructure.config.properties.RedisPresenceProperties;
import com.tinder.chat.util.IntegrationTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisUserPresenceAdapterIT extends IntegrationTestBase {

    @Autowired
    private RedisUserPresenceAdapter adapter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisPresenceProperties props;

    @Nested
    class IsUserOnline {

        @Test
        void isUserOnline_HasSessions_ReturnsTrue() {
            UUID userId = UUID.randomUUID();
            String key = props.sessionKeyPrefix() + userId;
            redisTemplate.opsForSet().add(key, "session-1", "session-2");

            boolean isOnline = adapter.isUserOnline(userId);

            assertTrue(isOnline);
        }

        @Test
        void isUserOnline_NoSessions_ReturnsFalse() {
            UUID userId = UUID.randomUUID();

            boolean isOnline = adapter.isUserOnline(userId);

            assertFalse(isOnline);
        }
    }

    @Nested
    class GetPresenceBatch {

        @Test
        void getPresenceBatch_MixedStates_ReturnsCorrectMapUsingPipelining() {
            UUID onlineUser1 = UUID.randomUUID();
            UUID onlineUser2 = UUID.randomUUID();
            UUID offlineUser = UUID.randomUUID();

            redisTemplate.opsForSet().add(props.sessionKeyPrefix() + onlineUser1, "sess-1");
            redisTemplate.opsForSet().add(props.sessionKeyPrefix() + onlineUser2, "sess-2");

            Set<UUID> usersToCheck = Set.of(onlineUser1, onlineUser2, offlineUser);

            Map<UUID, Boolean> result = adapter.getPresenceBatch(usersToCheck);

            assertEquals(3, result.size());
            assertTrue(result.get(onlineUser1));
            assertTrue(result.get(onlineUser2));
            assertFalse(result.get(offlineUser));
        }

        @Test
        void getPresenceBatch_EmptyInput_ReturnsEmptyMap() {
            Map<UUID, Boolean> result = adapter.getPresenceBatch(Set.of());
            assertTrue(result.isEmpty());
        }
    }
}