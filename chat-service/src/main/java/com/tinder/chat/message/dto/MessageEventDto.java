package com.tinder.chat.message.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageEventDto(
        Long id,
        UUID chatId,
        UUID senderId,
        UUID recipientId,
        ReplyInfoDto replyTo,
        String contentType,
        String content,
        Instant createdAt,
        Instant deletedAt
) {
}