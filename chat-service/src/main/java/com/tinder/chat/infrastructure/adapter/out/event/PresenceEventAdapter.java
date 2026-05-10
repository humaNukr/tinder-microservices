package com.tinder.chat.infrastructure.adapter.out.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.application.port.out.presence.PresenceEventPort;
import com.tinder.chat.infrastructure.config.properties.KafkaTopicsProperties;
import com.tinder.chat.infrastructure.config.properties.RedisPresenceProperties;
import com.tinder.chat.shared.dto.event.UserPresenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceEventAdapter implements PresenceEventPort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisPresenceProperties redisPresenceProperties;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties topicsProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void broadcastToChatServers(UserPresenceEvent event) {
        try {
            redisTemplate.convertAndSend(redisPresenceProperties.channel(), event);
        } catch (Exception e) {
            log.error("Failed to publish presence event to Redis", e);
        }
    }

    @Override
    public void broadcastToSystem(UserPresenceEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            log.debug("Sending offline presence event to Kafka topic {}: {}", topicsProperties.userPresenceEvents(), payload);
            kafkaTemplate.send(topicsProperties.userPresenceEvents(), event.userId().toString(), payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize UserPresenceEvent for Kafka: {}", event, e);
        } catch (Exception e) {
            log.error("Failed to publish presence event to Kafka", e);
        }
    }
}