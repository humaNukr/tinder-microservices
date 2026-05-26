package com.tinder.notification.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.notification.event.MessageSentEvent;
import com.tinder.notification.processor.MessageNotificationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventListener {

    private final MessageNotificationProcessor notificationFacade;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.message-events}",
            groupId = "${app.kafka.consumer-groups.notification-service}"
    )
    public void handleMessageEvent(String payload) throws JsonProcessingException {
        MessageSentEvent event = objectMapper.readValue(payload, MessageSentEvent.class);
        log.info("Successfully deserialized MessageSentEvent from Kafka. EventID: {}", event.eventId());
        notificationFacade.process(event);
    }
}
