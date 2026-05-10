package com.tinder.chat.shared.dto.event;

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