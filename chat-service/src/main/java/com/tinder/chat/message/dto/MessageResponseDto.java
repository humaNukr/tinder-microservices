package com.tinder.chat.message.dto;

import com.tinder.chat.message.enums.MessageContentType;
import java.time.Instant;
import java.util.UUID;

public record MessageResponseDto(
        Long id,
        UUID senderId,
        MessageContentType type,
        String content,
        Instant createdAt
) {}