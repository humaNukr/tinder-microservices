package com.tinder.chat.chat.dto;

import java.util.UUID;

public record MediaInitResponse(
        UUID localId,
        Long dbId,
        String uploadUrl
) {
}