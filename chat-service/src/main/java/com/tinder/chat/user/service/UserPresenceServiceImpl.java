package com.tinder.chat.user.service;

import com.tinder.chat.chat.port.UserPresencePublisher;
import com.tinder.chat.config.RedisPresenceProperties;
import com.tinder.chat.infrastructure.kafka.contract.UserPresenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPresenceServiceImpl implements UserPresenceService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisPresenceProperties presenceProperties;

    private final List<UserPresencePublisher> publishers;

    @Override
    public void userConnected(UUID userId, String sessionId) {
        String key = presenceProperties.sessionKeyPrefix() + userId;
        Long activeSessions = stringRedisTemplate.opsForSet().add(key, sessionId);

        stringRedisTemplate.expire(key, Duration.ofHours(24));

        if (activeSessions != null && activeSessions == 1L) {
            publishEvents(userId, true);
        }
    }

    @Override
    public void userDisconnected(UUID userId, String sessionId) {
        String key = presenceProperties.sessionKeyPrefix() + userId;
        stringRedisTemplate.opsForSet().remove(key, sessionId);

        Long remainingSessions = stringRedisTemplate.opsForSet().size(key);

        if (remainingSessions == null || remainingSessions == 0L) {
            publishEvents(userId, false);
        }
    }

    public boolean isUserOnline(UUID userId) {
        String key = presenceProperties.sessionKeyPrefix() + userId;
        Long sessions = stringRedisTemplate.opsForSet().size(key);
        return sessions != null && sessions > 0;
    }

    private void publishEvents(UUID userId, boolean isOnline) {
        UserPresenceEvent event = new UserPresenceEvent(userId, isOnline, LocalDateTime.now());
        log.info("Presence changed for user {}. Online: {}. Publishing...", userId, isOnline);

        for (UserPresencePublisher publisher : publishers) {
            try {
                publisher.publishUserPresenceEvent(event);
            } catch (Exception e) {
                log.error("Failed to publish presence event via {}", publisher.getClass().getSimpleName(), e);
            }
        }
    }
}