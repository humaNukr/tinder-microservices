package com.tinder.chat.infrastructure.redis;

import com.tinder.chat.config.RedisPresenceProperties;
import com.tinder.chat.user.service.UserPresenceService;
import com.tinder.chat.user.service.UserPresenceServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class PresenceGracePeriodListener extends KeyExpirationEventMessageListener {

    private final UserPresenceService userPresenceService;
    private final RedisPresenceProperties presenceProperties;

    public PresenceGracePeriodListener(
            RedisMessageListenerContainer listenerContainer,
            UserPresenceService userPresenceService,
            RedisPresenceProperties presenceProperties) {
        super(listenerContainer);
        this.userPresenceService = userPresenceService;
        this.presenceProperties = presenceProperties;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        String gracePrefix = presenceProperties.gracePeriodPrefix();

        if (expiredKey.startsWith(gracePrefix)) {
            try {
                String userIdStr = expiredKey.substring(gracePrefix.length());
                UUID userId = UUID.fromString(userIdStr);

                userPresenceService.handleGracePeriodExpired(userId);
            } catch (IllegalArgumentException e) {
                log.error("Failed to parse UUID from expired key: {}", expiredKey);
            }
        }
    }
}