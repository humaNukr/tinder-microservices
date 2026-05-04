package com.tinder.chat.chat.dto;

import java.util.UUID;

public record ReadReceiptEventDto(
        UUID chatId,
        UUID readerId,
        UUID recipientId,
        Long messageId
) {}