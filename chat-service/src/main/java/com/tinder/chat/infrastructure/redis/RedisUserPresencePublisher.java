package com.tinder.chat.infrastructure.redis;

import com.tinder.chat.chat.port.UserPresencePublisher;
import com.tinder.chat.config.RedisPresenceProperties;
import com.tinder.chat.infrastructure.kafka.contract.UserPresenceEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;


@Component("redisUserPresencePublisher")
@RequiredArgsConstructor
public class RedisUserPresencePublisher implements UserPresencePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisPresenceProperties redisPresenceProperties;

    @Override
    public void publishUserPresenceEvent(UserPresenceEvent event) {
        redisTemplate.convertAndSend(redisPresenceProperties.channel(), event);
    }
}
