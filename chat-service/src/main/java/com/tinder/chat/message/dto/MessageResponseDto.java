package com.tinder.chat.message.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageResponseDto(
        Long id,
        UUID senderId,
        String type,
        String content,
        Instant createdAt,
        Instant deletedAt
) {
}