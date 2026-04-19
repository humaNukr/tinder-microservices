package com.tinder.feed.listener;

import com.tinder.feed.event.SwipeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwipeHistoryListener {

    private final StringRedisTemplate redisTemplate;

    @KafkaListener(topics = "swipe-events", groupId = "feed-service-group")
    public void consumeSwipeEvent(SwipeCreatedEvent event) {
        String redisKey = "user:" + event.swiperId() + ":history";
        String swipedUserId = event.swipedId().toString();

        redisTemplate.opsForSet().add(redisKey, swipedUserId);
        redisTemplate.expire(redisKey, Duration.ofDays(30));

        log.debug("Added user {} to swipe history of user {}", swipedUserId, event.swiperId());
    }
}