package com.tinder.chat.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ChatPreview(
        UUID chatId,
        UUID partnerId,
        String lastMessageContent,
        String lastMessageType,
        Instant lastMessageCreatedAt,
        UUID lastMessageSenderId,
        Integer unreadCount
) {
}