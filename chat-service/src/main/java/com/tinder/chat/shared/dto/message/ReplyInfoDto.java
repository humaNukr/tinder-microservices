package com.tinder.chat.shared.dto.message;

import java.util.UUID;

public record ReplyInfoDto(
        Long messageId,
        UUID senderId,
        String type,
        String content
) {
}