package com.tinder.chat.infrastructure.adapter.out.redis;

import com.tinder.chat.infrastructure.config.properties.RedisPresenceProperties;
import com.tinder.chat.util.IntegrationTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisPresenceStateAdapterIT extends IntegrationTestBase {

    @Autowired
    private RedisPresenceStateAdapter adapter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisPresenceProperties props;

    @Nested
    class RegisterSessionAndClearGrace {

        @Test
        void registerSession_FirstSession_ReturnsTrueAndClearsGrace() {
            UUID userId = UUID.randomUUID();
            String sessionId = "sess-1";
            String graceKey = props.gracePeriodPrefix() + userId;
            String sessionKey = props.sessionKeyPrefix() + userId;

            redisTemplate.opsForValue().set(graceKey, "pending");

            boolean isFirstSession = adapter.registerSessionAndClearGrace(userId, sessionId);

            assertTrue(isFirstSession);

            assertFalse(redisTemplate.hasKey(graceKey));

            assertTrue(redisTemplate.opsForSet().isMember(sessionKey, sessionId));
            Long expire = redisTemplate.getExpire(sessionKey);
            assertTrue(expire != null && expire > 0);
        }

        @Test
        void registerSession_SecondSession_ReturnsFalse() {
            UUID userId = UUID.randomUUID();
            String sessionKey = props.sessionKeyPrefix() + userId;

            redisTemplate.opsForSet().add(sessionKey, "existing-sess");

            boolean isFirstSession = adapter.registerSessionAndClearGrace(userId, "new-sess");

            assertFalse(isFirstSession);
            assertEquals(2, redisTemplate.opsForSet().size(sessionKey));
        }
    }

    @Nested
    class UnregisterSession {

        @Test
        void unregisterSession_LastSession_ReturnsTrue() {
            UUID userId = UUID.randomUUID();
            String sessionId = "sess-1";
            String sessionKey = props.sessionKeyPrefix() + userId;

            redisTemplate.opsForSet().add(sessionKey, sessionId);

            boolean isOfflineNow = adapter.unregisterSession(userId, sessionId);

            assertTrue(isOfflineNow);
            assertEquals(0, redisTemplate.opsForSet().size(sessionKey));
        }

        @Test
        void unregisterSession_OtherSessionsExist_ReturnsFalse() {
            UUID userId = UUID.randomUUID();
            String sessionKey = props.sessionKeyPrefix() + userId;

            redisTemplate.opsForSet().add(sessionKey, "sess-1", "sess-2");

            boolean isOfflineNow = adapter.unregisterSession(userId, "sess-1");

            assertFalse(isOfflineNow);
            assertTrue(redisTemplate.opsForSet().isMember(sessionKey, "sess-2"));
        }
    }

    @Nested
    class StartGracePeriod {

        @Test
        void startGracePeriod_SetsValueWithTTL() {
            UUID userId = UUID.randomUUID();
            String graceKey = props.gracePeriodPrefix() + userId;

            adapter.startGracePeriod(userId);

            assertEquals("pending", redisTemplate.opsForValue().get(graceKey));

            Long expire = redisTemplate.getExpire(graceKey);
            assertNotNull(expire);
            assertTrue(expire > 0 && expire <= props.gracePeriodDuration().getSeconds());
        }
    }
}