package com.tinder.swipe.util;

import com.tinder.swipe.dto.swipe.SwipeRequestDto;
import com.tinder.swipe.event.MatchEvent;
import com.tinder.swipe.event.SwipeCreatedEvent;

import java.time.Instant;
import java.util.UUID;

public final class SwipeTestFixtures {

    public static final UUID USER_ONE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID USER_TWO = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID USER_THREE = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID USER_FOUR = UUID.fromString("00000000-0000-0000-0000-000000000004");
    public static final UUID USER_FIVE = UUID.fromString("00000000-0000-0000-0000-000000000005");

    private SwipeTestFixtures() {
    }

    public static SwipeRequestDto like(UUID targetId) {
        return new SwipeRequestDto(targetId, true);
    }

    public static SwipeRequestDto dislike(UUID targetId) {
        return new SwipeRequestDto(targetId, false);
    }

    public static SwipeCreatedEvent swipeCreatedEvent(UUID swiperId, UUID swipedId, boolean isLiked) {
        return new SwipeCreatedEvent(swiperId, swipedId, isLiked, Instant.parse("2026-01-15T12:00:00Z"));
    }

    public static MatchEvent matchEvent(UUID eventId, UUID user1Id, UUID user2Id) {
        return new MatchEvent(eventId, user1Id, user2Id);
    }
}
