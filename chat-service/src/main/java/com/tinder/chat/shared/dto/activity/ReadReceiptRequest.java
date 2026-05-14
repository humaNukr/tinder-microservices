package com.tinder.chat.shared.dto.activity;

import java.util.UUID;

public record ReadReceiptRequest(
        UUID chatId,
        Long messageId
) {
}

