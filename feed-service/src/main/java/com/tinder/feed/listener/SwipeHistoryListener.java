package com.tinder.feed.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.feed.event.SwipeCreatedEvent;
import com.tinder.feed.service.interfaces.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwipeHistoryListener {

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.swipe-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeSwipeEvent(String payload) {
        try {
            SwipeCreatedEvent event = objectMapper.readValue(payload, SwipeCreatedEvent.class);
            redisService.addSwipedUserToHistory(event.swiperId(), event.swipedId());
            log.debug("Added user {} to swipe history of user {}", event.swipedId(), event.swiperId());

        } catch (Exception e) {
            log.error("Failed to parse SwipeCreatedEvent from Kafka payload: {}", payload, e);
        }
    }
}