package com.tinder.chat.chat.dto;

import java.util.UUID;

public record ReadReceiptRequest(
        UUID chatId,
        Long messageId
) {
}

