package com.tinder.chat.message.dto;

import java.util.UUID;

public record ReplyInfoDto(
        Long messageId,
        UUID senderId,
        String type,
        String content
) {}