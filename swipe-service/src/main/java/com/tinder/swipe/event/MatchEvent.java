package com.tinder.swipe.event;


import java.util.UUID;

public record MatchEvent(
        UUID user1Id,
        UUID user2Id
) {
}
