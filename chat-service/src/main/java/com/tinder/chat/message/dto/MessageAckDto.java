package com.tinder.chat.message.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageAckDto(
        UUID localId,
        Long dbId,
        Instant createdAt
) {
}