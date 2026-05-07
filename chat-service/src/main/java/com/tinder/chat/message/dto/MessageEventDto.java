package com.tinder.chat.message.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageEventDto(
        Long id,
        UUID chatId,
        UUID senderId,
        UUID recipientId,
        ReplyInfoDto replyTo,
        String type,
        String content,
        List<ReactionInfoDto> reactions,
        Instant createdAt,
        Instant deletedAt
) {
}