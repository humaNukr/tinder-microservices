package com.tinder.chat.message.dto;

import java.util.UUID;

public record ReactionInfoDto(
        UUID userId,
        String reaction
) {
}