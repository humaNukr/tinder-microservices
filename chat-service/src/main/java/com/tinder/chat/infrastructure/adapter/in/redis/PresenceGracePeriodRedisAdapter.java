package com.tinder.chat.infrastructure.adapter.in.redis;

import com.tinder.chat.application.port.in.presence.UpdatePresenceUseCase;
import com.tinder.chat.infrastructure.config.properties.RedisPresenceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class PresenceGracePeriodRedisAdapter extends KeyExpirationEventMessageListener {

    private final UpdatePresenceUseCase updatePresenceUseCase;
    private final RedisPresenceProperties presenceProperties;

    public PresenceGracePeriodRedisAdapter(
            RedisMessageListenerContainer listenerContainer,
            UpdatePresenceUseCase updatePresenceUseCase,
            RedisPresenceProperties presenceProperties) {
        super(listenerContainer);
        this.updatePresenceUseCase = updatePresenceUseCase;
        this.presenceProperties = presenceProperties;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        String gracePrefix = presenceProperties.gracePeriodPrefix();

        if (expiredKey.startsWith(gracePrefix)) {
            try {
                String userIdStr = expiredKey.substring(gracePrefix.length());
                updatePresenceUseCase.handleGracePeriodExpired(UUID.fromString(userIdStr));
            } catch (IllegalArgumentException e) {
                log.error("Failed to parse UUID from expired key: {}", expiredKey);
            }
        }
    }
}