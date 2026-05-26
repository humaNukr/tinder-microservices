package com.tinder.swipe.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.swipe.event.ActivityType;
import com.tinder.swipe.event.UserActivityEvent;
import com.tinder.swipe.processor.AccountDeletionProcessor;
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
            groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserActivity(String payload) throws Exception {
        UserActivityEvent event = objectMapper.readValue(payload, UserActivityEvent.class);

        if (event.type() == ActivityType.DELETE_ACCOUNT) {
            accountDeletionProcessor.process(event);
        }
    }
}
