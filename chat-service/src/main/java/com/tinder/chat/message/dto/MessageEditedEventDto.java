package com.tinder.chat.message.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageEditedEventDto(
        Long messageId,
        UUID chatId,
        UUID senderId,
        UUID recipientId,
        String newContent,
        Instant editedAt
) {
}