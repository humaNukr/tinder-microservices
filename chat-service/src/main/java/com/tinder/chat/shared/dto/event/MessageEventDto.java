package com.tinder.chat.shared.dto.event;

import com.tinder.chat.shared.dto.message.ReactionInfoDto;
import com.tinder.chat.shared.dto.message.ReplyInfoDto;

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