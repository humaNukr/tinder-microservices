package com.tinder.profile.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.profile.event.ActivityType;
import com.tinder.profile.event.UserActivityEvent;
import com.tinder.profile.processor.UserActivityProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityListener {

    private final UserActivityProcessor userActivityProcessor;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.user-activity}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "3"
    )
    public void handleUserActivity(String payload) {
        try {
            UserActivityEvent event = objectMapper.readValue(payload, UserActivityEvent.class);

            if (event.type() == ActivityType.DELETE_ACCOUNT) {
                userActivityProcessor.deleteProfileData(event);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event", e);
        }
    }
}