package com.tinder.notification.listener;

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

    @KafkaListener(topics = "match-events", groupId = "notification-group")
    public void handleMatchEvent(MatchEvent event) {
        log.info("Received MatchEvent from Kafka. EventID: {}", event.eventId());

        matchNotificationProcessor.process(
                event.eventId(),
                event.user1Id(),
                event.user2Id()
        );
    }
}