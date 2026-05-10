package com.tinder.chat.infrastructure.adapter.out.redis;

import com.tinder.chat.application.port.out.presence.PresenceStatePort;
import com.tinder.chat.infrastructure.config.properties.RedisPresenceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisPresenceStateAdapter implements PresenceStatePort {

    private final StringRedisTemplate redisTemplate;
    private final RedisPresenceProperties props;

    @Override
    public boolean registerSessionAndClearGrace(UUID userId, String sessionId) {
        String sessionKey = props.sessionKeyPrefix() + userId;
        String graceKey = props.gracePeriodPrefix() + userId;

        redisTemplate.delete(graceKey);
        redisTemplate.opsForSet().add(sessionKey, sessionId);
        redisTemplate.expire(sessionKey, Duration.ofHours(24));

        Long size = redisTemplate.opsForSet().size(sessionKey);
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