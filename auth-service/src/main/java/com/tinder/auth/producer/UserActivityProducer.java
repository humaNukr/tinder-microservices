package com.tinder.auth.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.auth.event.ActivityType;
import com.tinder.auth.event.UserActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.user-activity}")
    private String activityTopic;

    public void publishActivity(UUID userId, ActivityType type) {
        try {
            UserActivityEvent event = new UserActivityEvent(UUID.randomUUID(), userId, type, Instant.now());
            kafkaTemplate.send(activityTopic, userId.toString(), objectMapper.writeValueAsString(event))
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to send user activity event for user {}: {}", userId, ex.getMessage());
                        } else {
                            log.debug("Published activity {} for user {}", type, userId);
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to serialize activity event for user {}", userId, e);
        }
    }
}
