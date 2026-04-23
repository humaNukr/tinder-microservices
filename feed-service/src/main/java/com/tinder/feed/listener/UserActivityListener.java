package com.tinder.feed.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.feed.event.ActivityType;
import com.tinder.feed.event.UserActivityEvent;
import com.tinder.feed.service.interfaces.DeckGeneratorService;
import com.tinder.feed.service.interfaces.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityListener {

    private final DeckGeneratorService deckGeneratorService;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.user-activity}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeActivityEvent(String payload) {
        try {
            UserActivityEvent event = objectMapper.readValue(payload, UserActivityEvent.class);

            log.debug("Received activity event {} for user {}", event.type(), event.userId());

            if (event.type() == ActivityType.LOCATION_UPDATE) {
                redisService.deleteDeck(event.userId());
                log.info("Deleted stale deck for user {} due to LOCATION_UPDATE", event.userId());
                deckGeneratorService.generateDeckAsync(event.userId());
            }

            if (event.type() == ActivityType.LOGIN) {
                log.info("Cache warming triggered for user {} due to activity {}", event.userId(), event.type());
                deckGeneratorService.generateDeckAsync(event.userId());
            }
        } catch (Exception e) {
            log.error("Failed to parse UserActivityEvent from Kafka payload: {}", payload, e);
        }
    }
}