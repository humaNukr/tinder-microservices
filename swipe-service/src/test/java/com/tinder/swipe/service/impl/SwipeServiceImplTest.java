package com.tinder.swipe.service.impl;

import com.tinder.swipe.dto.swipe.SwipeResponseDto;
import com.tinder.swipe.dto.swipe.SwipeStatusProjection;
import com.tinder.swipe.event.MatchEvent;
import com.tinder.swipe.event.SwipeCreatedEvent;
import com.tinder.swipe.exception.SwipeAlreadyExistsException;
import com.tinder.swipe.properties.KafkaProperties;
import com.tinder.swipe.repository.SwipeRepository;
import com.tinder.swipe.service.interfaces.OutboxService;
import com.tinder.swipe.service.interfaces.SwipeRateLimiterService;
import com.tinder.swipe.util.SwipeTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SwipeServiceImpl")
class SwipeServiceImplTest {

    @Mock
    private SwipeRepository swipeRepository;
    @Mock
    private OutboxService outboxService;
    @Mock
    private KafkaProperties kafkaProperties;
    @Mock
    private SwipeRateLimiterService rateLimiterService;

    @InjectMocks
    private SwipeServiceImpl swipeService;

    private static SwipeStatusProjection projection(Boolean likedByUser1, Boolean likedByUser2) {
        SwipeStatusProjection projection = mock(SwipeStatusProjection.class);
        lenient().when(projection.getIsLikedByUser1()).thenReturn(likedByUser1);
        lenient().when(projection.getIsLikedByUser2()).thenReturn(likedByUser2);
        return projection;
    }

    @Nested
    @DisplayName("processSwipe()")
    class ProcessSwipe {

        @Test
        @DisplayName("throws when actor swipes themselves")
        void selfSwipe_ThrowsIllegalArgumentException() {
            UUID userId = UUID.randomUUID();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> swipeService.processSwipe(userId, SwipeTestFixtures.like(userId)));

            verifyNoInteractions(swipeRepository, outboxService, rateLimiterService);
        }

        @Test
        @DisplayName("throws when user already swiped the target")
        void duplicateSwipe_ThrowsSwipeAlreadyExistsException() {
            when(swipeRepository.upsertSwipeByUser1(
                    SwipeTestFixtures.USER_ONE, SwipeTestFixtures.USER_TWO, true))
                    .thenReturn(null);

            assertThrows(
                    SwipeAlreadyExistsException.class,
                    () -> swipeService.processSwipe(
                            SwipeTestFixtures.USER_ONE, SwipeTestFixtures.like(SwipeTestFixtures.USER_TWO)));

            verify(outboxService, never()).saveEvent(any(), any());
        }

        @Test
        @DisplayName("returns no match and publishes swipe event on first like")
        void firstLike_ReturnsNoMatch() {
            SwipeStatusProjection projection = projection(true, null);
            when(kafkaProperties.swipe()).thenReturn("swipe-events");
            when(swipeRepository.upsertSwipeByUser1(
                    SwipeTestFixtures.USER_ONE, SwipeTestFixtures.USER_TWO, true))
                    .thenReturn(projection);

            SwipeResponseDto response = swipeService.processSwipe(
                    SwipeTestFixtures.USER_ONE, SwipeTestFixtures.like(SwipeTestFixtures.USER_TWO));

            assertAll(
                    () -> assertFalse(response.isMatched()),
                    () -> verify(rateLimiterService).checkAndIncrementLikeLimit(SwipeTestFixtures.USER_ONE, false),
                    () -> verify(outboxService).saveEvent(eq("swipe-events"), any(SwipeCreatedEvent.class)));
            verify(outboxService, never()).saveEvent(any(), any(MatchEvent.class));
        }

        @Test
        @DisplayName("does not apply rate limit on dislike")
        void dislike_SkipsRateLimiter() {
            SwipeStatusProjection projection = projection(false, null);
            when(kafkaProperties.swipe()).thenReturn("swipe-events");
            when(swipeRepository.upsertSwipeByUser1(
                    SwipeTestFixtures.USER_ONE, SwipeTestFixtures.USER_TWO, false))
                    .thenReturn(projection);

            SwipeResponseDto response = swipeService.processSwipe(
                    SwipeTestFixtures.USER_ONE, SwipeTestFixtures.dislike(SwipeTestFixtures.USER_TWO));

            assertFalse(response.isMatched());
            verify(rateLimiterService, never()).checkAndIncrementLikeLimit(any(), eq(false));
        }

        @Test
        @DisplayName("returns match and publishes match event on mutual like")
        void mutualLike_ReturnsMatch() {
            SwipeStatusProjection projection = projection(true, true);
            when(kafkaProperties.swipe()).thenReturn("swipe-events");
            when(kafkaProperties.match()).thenReturn("match-events");
            when(swipeRepository.upsertSwipeByUser2(
                    SwipeTestFixtures.USER_ONE, SwipeTestFixtures.USER_TWO, true))
                    .thenReturn(projection);

            SwipeResponseDto response = swipeService.processSwipe(
                    SwipeTestFixtures.USER_TWO, SwipeTestFixtures.like(SwipeTestFixtures.USER_ONE));

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxService).saveEvent(eq("swipe-events"), eventCaptor.capture());
            verify(outboxService).saveEvent(eq("match-events"), any(MatchEvent.class));

            assertAll(
                    () -> assertTrue(response.isMatched()),
                    () -> assertEquals(SwipeTestFixtures.USER_TWO, ((SwipeCreatedEvent) eventCaptor.getValue()).swiperId()));
        }
    }
}
