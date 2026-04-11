package com.tinder.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.notification.event.MatchEvent;
import com.tinder.notification.processor.MatchNotificationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchEventListener {

    private final MatchNotificationProcessor matchNotificationProcessor;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "match-events",
            groupId = "notification-group"
    )
    public void handleMatchEvent(String payload) {
        try {
            MatchEvent event = objectMapper.readValue(payload, MatchEvent.class);

            log.info("Successfully deserialized MatchEvent from Kafka. EventID: {}", event.eventId());

            matchNotificationProcessor.process(
                    event.eventId(),
                    event.user1Id(),
                    event.user2Id()
            );

        } catch (Exception e) {
            log.error("Failed to parse match event payload from Kafka: {}", payload, e);
        }
    }
}