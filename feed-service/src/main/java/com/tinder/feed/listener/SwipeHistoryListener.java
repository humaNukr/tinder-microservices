package com.tinder.feed.listener;

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

    @KafkaListener(topics = "${app.kafka.topics.swipe-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeSwipeEvent(SwipeCreatedEvent event) {
        redisService.addSwipedUserToHistory(event.swiperId(), event.swipedId());
        log.debug("Added user {} to swipe history of user {}", event.swipedId(), event.swiperId());
    }
}