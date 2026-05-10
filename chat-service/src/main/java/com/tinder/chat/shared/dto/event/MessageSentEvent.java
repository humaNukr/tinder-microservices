package com.tinder.chat.shared.dto.event;

import java.time.Instant;
import java.util.UUID;

public record MessageSentEvent(
        UUID eventId,
        Long id,
        UUID chatId,
        UUID senderId,
        UUID recipientId,
        String contentType,
        String contentSnippet,
        Instant createdAt
) {
}
