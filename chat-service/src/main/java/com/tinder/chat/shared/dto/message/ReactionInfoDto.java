package com.tinder.chat.shared.dto.message;

import java.util.UUID;

public record ReactionInfoDto(
        UUID userId,
        String reaction
) {
}