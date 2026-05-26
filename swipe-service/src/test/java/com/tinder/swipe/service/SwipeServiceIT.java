package com.tinder.swipe.service;

import com.tinder.swipe.dto.swipe.SwipeResponseDto;
import com.tinder.swipe.entity.OutboxEvent;
import com.tinder.swipe.exception.SwipeAlreadyExistsException;
import com.tinder.swipe.exception.TooManyRequestsException;
import com.tinder.swipe.properties.KafkaProperties;
import com.tinder.swipe.repository.OutboxRepository;
import com.tinder.swipe.repository.SwipeRepository;
import com.tinder.swipe.service.interfaces.SwipeService;
import com.tinder.swipe.util.BaseIT;
import com.tinder.swipe.util.SwipeTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SwipeService — Integration Tests")
class SwipeServiceIT extends BaseIT {

    @Autowired
    private SwipeService swipeService;

    @Autowired
    private SwipeRepository swipeRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private KafkaProperties kafkaProperties;

    @BeforeEach
    void setUp() {
        swipeRepository.deleteAll();
        outboxRepository.deleteAll();
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Nested
    @DisplayName("processSwipe()")
    class ProcessSwipe {

        @Test
        @DisplayName("persists swipe and outbox event on first like")
        void firstLike_PersistsSwipeAndOutbox() {
            SwipeResponseDto response = swipeService.processSwipe(
                    SwipeTestFixtures.USER_ONE, SwipeTestFixtures.like(SwipeTestFixtures.USER_TWO));

            List<OutboxEvent> events = outboxRepository.findAll();

            assertAll(
                    () -> assertFalse(response.isMatched()),
                    () -> assertEquals(1, swipeRepository.count()),
                    () -> assertEquals(1, events.size()),
                    () -> assertEquals(kafkaProperties.swipe(), events.getFirst().getTopic()));
        }

        @Test
        @DisplayName("returns match and writes match outbox event on mutual like")
        void mutualLike_ReturnsMatch() {
            swipeService.processSwipe(
                    SwipeTestFixtures.USER_ONE, SwipeTestFixtures.like(SwipeTestFixtures.USER_TWO));

            SwipeResponseDto response = swipeService.processSwipe(
                    SwipeTestFixtures.USER_TWO, SwipeTestFixtures.like(SwipeTestFixtures.USER_ONE));

            long matchEvents = outboxRepository.findAll().stream()
                    .filter(event -> kafkaProperties.match().equals(event.getTopic()))
                    .count();

            assertAll(
                    () -> assertTrue(response.isMatched()),
                    () -> assertEquals(3, outboxRepository.count()),
                    () -> assertEquals(1, matchEvents));
        }

        @Test
        @DisplayName("throws when swiping the same user twice")
        void duplicateSwipe_ThrowsConflict() {
            swipeService.processSwipe(
                    SwipeTestFixtures.USER_ONE, SwipeTestFixtures.like(SwipeTestFixtures.USER_TWO));

            assertThrows(
                    SwipeAlreadyExistsException.class,
                    () -> swipeService.processSwipe(
                            SwipeTestFixtures.USER_ONE, SwipeTestFixtures.like(SwipeTestFixtures.USER_TWO)));
        }

        @Test
        @DisplayName("enforces daily like limit via Redis")
        void likeLimitExceeded_ThrowsTooManyRequests() {
            swipeService.processSwipe(
                    SwipeTestFixtures.USER_ONE, SwipeTestFixtures.like(SwipeTestFixtures.USER_TWO));
            swipeService.processSwipe(
                    SwipeTestFixtures.USER_ONE, SwipeTestFixtures.like(SwipeTestFixtures.USER_THREE));
            swipeService.processSwipe(
                    SwipeTestFixtures.USER_ONE, SwipeTestFixtures.like(SwipeTestFixtures.USER_FOUR));

            assertThrows(
                    TooManyRequestsException.class,
                    () -> swipeService.processSwipe(
                            SwipeTestFixtures.USER_ONE, SwipeTestFixtures.like(SwipeTestFixtures.USER_FIVE)));
        }
    }
}
