package com.tinder.notification.event;

import java.util.UUID;

public record MessageEvent(
        UUID eventId,
        UUID messageId,
        UUID chatId,
        UUID senderId,
        UUID recipientId,
        String contentSnippet
) {
}