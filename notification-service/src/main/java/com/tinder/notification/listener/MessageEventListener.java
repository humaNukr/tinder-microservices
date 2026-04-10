package com.tinder.notification.listener;

import com.tinder.notification.event.MessageEvent;
import com.tinder.notification.processor.MessageNotificationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventListener {

    private final MessageNotificationProcessor messageNotificationProcessor;

    @KafkaListener(topics = "message-events", groupId = "notification-group")
    public void handleMessageEvent(MessageEvent event) {
        log.info("Received MessageEvent from Kafka. EventID: {}", event.eventId());
        messageNotificationProcessor.process(event);
    }
}