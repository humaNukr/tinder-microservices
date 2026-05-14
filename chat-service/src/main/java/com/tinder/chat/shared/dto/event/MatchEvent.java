package com.tinder.chat.shared.dto.event;

import java.util.UUID;

public record MatchEvent(
        UUID eventId,
        UUID user1Id,
        UUID user2Id
) {
}
