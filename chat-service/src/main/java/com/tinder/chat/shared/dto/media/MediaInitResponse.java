package com.tinder.chat.shared.dto.media;

import java.util.UUID;

public record MediaInitResponse(
        UUID localId,
        Long dbId,
        String uploadUrl
) {
}