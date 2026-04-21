package com.tinder.feed.listener;

import com.tinder.feed.event.ActivityType;
import com.tinder.feed.event.UserActivityEvent;
import com.tinder.feed.properties.FeedProperties;
import com.tinder.feed.service.interfaces.DeckGeneratorService;
import com.tinder.feed.service.interfaces.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityListener {

    private final RedisService redisService;
    private final DeckGeneratorService deckGeneratorService;
    private final FeedProperties properties;

    @KafkaListener(topics = "${app.kafka.topics.user-activity}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeActivityEvent(UserActivityEvent event) {
        log.debug("Received activity event {} for user {}", event.type(), event.userId());

        if (event.type() == ActivityType.LOCATION_UPDATE) {
            redisService.deleteDeck(event.userId());
            log.info("Deleted stale deck for user {} due to LOCATION_UPDATE", event.userId());

            deckGeneratorService.generateDeckAsync(event.userId());

        } else {
            Long currentDeckSize = redisService.getDeckSize(event.userId());
            if (currentDeckSize == null || currentDeckSize < properties.refillThreshold()) {
                log.info("Cache warming triggered for user {} due to activity {}", event.userId(), event.type());
                deckGeneratorService.generateDeckAsync(event.userId());
            }
        }
    }
}