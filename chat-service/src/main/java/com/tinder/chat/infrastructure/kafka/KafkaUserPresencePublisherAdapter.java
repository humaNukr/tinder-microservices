package com.tinder.chat.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.chat.port.UserPresencePublisher;
import com.tinder.chat.config.KafkaTopicsProperties;
import com.tinder.chat.infrastructure.kafka.contract.UserPresenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUserPresencePublisherAdapter implements UserPresencePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties topicsProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void publishUserPresenceEvent(UserPresenceEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            log.debug("Sending offline presence event to Kafka topic {}: {}", topicsProperties.userPresenceEvents(), payload);
            kafkaTemplate.send(topicsProperties.userPresenceEvents(), event.userId().toString(), payload);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize UserPresenceEvent for Kafka: {}", event, e);
        }
    }
}