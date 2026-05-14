package com.tinder.chat.shared.dto.event;

import java.time.Instant;
import java.util.UUID;

public record MessageAckDto(
        UUID localId,
        Long dbId,
        Instant createdAt
) {
}