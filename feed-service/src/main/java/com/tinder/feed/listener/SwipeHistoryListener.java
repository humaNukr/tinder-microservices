package com.tinder.feed.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.feed.event.SwipeCreatedEvent;
import com.tinder.feed.service.interfaces.FeedStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwipeHistoryListener {

    private final FeedStorageService feedStorageService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.swipe-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeSwipeEvent(String payload) throws Exception {
        SwipeCreatedEvent event = objectMapper.readValue(payload, SwipeCreatedEvent.class);

        feedStorageService.addSwipedUserToHistory(event.swiperId(), event.swipedId());
        log.debug("Added user {} to swipe history of user {}", event.swipedId(), event.swiperId());
    }
}