package com.tinder.chat.infrastructure.adapter.out.redis;

import com.tinder.chat.application.port.out.presence.PresenceStatePort;
import com.tinder.chat.infrastructure.config.properties.RedisPresenceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisPresenceStateAdapter implements PresenceStatePort {

    private final StringRedisTemplate redisTemplate;
    private final RedisPresenceProperties props;

    public boolean registerSessionAndClearGrace(UUID userId, String sessionId) {
        String graceKey = props.gracePeriodPrefix() + userId;
        String sessionKey = props.gracePeriodPrefix() + userId;

        List<Object> results = redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> Object execute(RedisOperations<K, V> operations) {
                StringRedisTemplate stringOps = (StringRedisTemplate) operations;
                stringOps.delete(graceKey);
                stringOps.opsForSet().add(sessionKey, sessionId);
                stringOps.expire(sessionKey, Duration.ofHours(24));
                stringOps.opsForSet().size(sessionKey);
                return null;
            }
        });
        Long size = (Long) results.get(3);
        return size != null && size == 1L;
    }

    @Override
    public boolean unregisterSession(UUID userId, String sessionId) {
        String sessionKey = props.sessionKeyPrefix() + userId;
        redisTemplate.opsForSet().remove(sessionKey, sessionId);

        Long size = redisTemplate.opsForSet().size(sessionKey);
        return size == null || size == 0L;
    }

    @Override
    public void startGracePeriod(UUID userId) {
        String graceKey = props.gracePeriodPrefix() + userId;
        redisTemplate.opsForValue().set(graceKey, "pending", props.gracePeriodDuration());
    }

    @Override
    public boolean isOffline(UUID userId) {
        String sessionKey = props.sessionKeyPrefix() + userId;
        Long size = redisTemplate.opsForSet().size(sessionKey);
        return size == null || size == 0L;
    }
}