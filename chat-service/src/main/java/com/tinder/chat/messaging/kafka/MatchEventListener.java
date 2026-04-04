package com.tinder.chat.messaging.kafka;

import com.tinder.chat.messaging.inbox.MatchEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchEventListener {

    private final MatchEventProcessor eventProcessor;

    @KafkaListener(
            topics = "${app.kafka.topics.match-events}",
            groupId = "${app.kafka.consumer-groups.chat-service}"
    )
    public void listenMatchEvent(MatchEvent event) {
        eventProcessor.processMatchEvent(event);
    }
}