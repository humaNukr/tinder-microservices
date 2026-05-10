package com.tinder.chat.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicsProperties(
        String matchEvents,
        String messageEvents,
        String minioChatMediaEvents,
        String userPresenceEvents
) {
}
