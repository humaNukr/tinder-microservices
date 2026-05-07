package com.tinder.chat.message.dto;

import java.util.UUID;

public record ReactionEventDto(
        UUID chatId,
        Long messageId,
        UUID senderId,
        UUID recipientId,
        String reaction,
        boolean isRemoved
) {
}