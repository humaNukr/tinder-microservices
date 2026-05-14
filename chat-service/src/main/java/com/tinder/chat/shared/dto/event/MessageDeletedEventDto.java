package com.tinder.chat.shared.dto.event;

import java.time.Instant;
import java.util.UUID;

public record MessageDeletedEventDto(
        Long messageId,
        UUID chatId,
        UUID senderId,
        UUID recipientId,
        Instant deletedAt
) {
}