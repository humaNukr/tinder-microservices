package com.tinder.chat.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.application.service.account.AccountDeletionProcessor;
import com.tinder.chat.domain.enums.ActivityType;
import com.tinder.chat.shared.dto.event.UserActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityKafkaAdapter {

    private final AccountDeletionProcessor accountDeletionProcessor;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.user-activity}",
            groupId = "${app.kafka.consumer-groups.chat-service}")
    public void listen(String payload) throws JsonProcessingException {
        UserActivityEvent event = objectMapper.readValue(payload, UserActivityEvent.class);

        if (event.type() == ActivityType.DELETE_ACCOUNT) {
            accountDeletionProcessor.process(event);
        }
    }
}
