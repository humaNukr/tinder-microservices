package com.tinder.chat.messaging.kafka;

import java.time.Instant;
import java.util.UUID;

public record MatchEvent
(
        UUID eventId,
        UUID user1Id,
        UUID user2Id,
        Instant matchedAt
) {
}
