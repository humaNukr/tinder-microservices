package com.tinder.chat.message.dto;

import java.util.UUID;

public record ChatRequestDto(
        UUID chatId,
        String payload,
        String type
) {
}
