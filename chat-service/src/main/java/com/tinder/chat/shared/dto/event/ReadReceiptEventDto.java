package com.tinder.chat.shared.dto.event;

import java.util.UUID;

public record ReadReceiptEventDto(
        UUID chatId,
        UUID readerId,
        UUID recipientId,
        Long messageId
) {
}