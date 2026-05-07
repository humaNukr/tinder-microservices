package com.tinder.chat.user.service;

import com.tinder.chat.chat.port.UserPresencePublisher;
import com.tinder.chat.config.RedisPresenceProperties;
import com.tinder.chat.infrastructure.kafka.contract.UserPresenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPresenceServiceImpl implements UserPresenceService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisPresenceProperties presenceProperties;

    @Qualifier("redisUserPresencePublisher")
    private final UserPresencePublisher redisUserPresencePublisher;

    @Qualifier("kafkaUserPresencePublisherAdapter")
    private final UserPresencePublisher kafkaUserPresencePublisherAdapter;

    @Override
    public void userConnected(UUID userId, String sessionId) {
        String sessionKey = presenceProperties.sessionKeyPrefix() + userId;
        String graceKey = presenceProperties.gracePeriodPrefix() + userId;

        stringRedisTemplate.delete(graceKey);

        Long activeSessions = stringRedisTemplate.opsForSet().add(sessionKey, sessionId);
        stringRedisTemplate.expire(sessionKey, Duration.ofHours(24));

        if (activeSessions != null && activeSessions == 1L) {
            publishEvents(userId, true);
        }
    }

    @Override
    public void userDisconnected(UUID userId, String sessionId) {
        String sessionKey = presenceProperties.sessionKeyPrefix() + userId;
        String graceKey = presenceProperties.gracePeriodPrefix() + userId;

        stringRedisTemplate.opsForSet().remove(sessionKey, sessionId);
        Long remainingSessions = stringRedisTemplate.opsForSet().size(sessionKey);

        if (remainingSessions == null || remainingSessions == 0L) {
            log.debug("User {} disconnected. Starting grace period.", userId);
            stringRedisTemplate.opsForValue().set(graceKey, "pending", presenceProperties.gracePeriodDuration());
        }
    }

    @Override
    public void handleGracePeriodExpired(UUID userId) {
        String sessionKey = presenceProperties.sessionKeyPrefix() + userId;
        Long remainingSessions = stringRedisTemplate.opsForSet().size(sessionKey);

        if (remainingSessions == null || remainingSessions == 0L) {
            log.debug("Grace period expired for user {}. Publishing offline event.", userId);
            publishEvents(userId, false);
        }
    }

    @Override
    public boolean isUserOnline(UUID userId) {
        String key = presenceProperties.sessionKeyPrefix() + userId;
        Long sessions = stringRedisTemplate.opsForSet().size(key);
        return sessions != null && sessions > 0;
    }

    private void publishEvents(UUID userId, boolean isOnline) {
        UserPresenceEvent event = new UserPresenceEvent(userId, isOnline, LocalDateTime.now());

        try {
            redisUserPresencePublisher.publishUserPresenceEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish presence event to Redis", e);
        }

        if (!isOnline) {
            try {
                kafkaUserPresencePublisherAdapter.publishUserPresenceEvent(event);
            } catch (Exception e) {
                log.error("Failed to publish presence event to Kafka", e);
            }
        }
    }
}