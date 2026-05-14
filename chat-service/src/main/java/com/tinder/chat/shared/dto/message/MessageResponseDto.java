package com.tinder.chat.shared.dto.message;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponseDto(
        Long id,
        UUID senderId,
        ReplyInfoDto replyTo,
        String type,
        String content,
        List<ReactionInfoDto> reactions,
        Instant createdAt,
        Instant deletedAt
) {
}