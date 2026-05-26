package com.tinder.notification.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.notification.event.ActivityType;
import com.tinder.notification.event.UserActivityEvent;
import com.tinder.notification.processor.AccountDeletionProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityListener {

    private final AccountDeletionProcessor accountDeletionProcessor;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.user-activity}",
            groupId = "${app.kafka.consumer-groups.notification-service}")
    public void handleUserActivity(String payload) throws JsonProcessingException {
        UserActivityEvent event = objectMapper.readValue(payload, UserActivityEvent.class);

        if (event.type() == ActivityType.DELETE_ACCOUNT) {
            accountDeletionProcessor.process(event);
        }
    }
}
