package com.tinder.chat.chat.dto;

import java.util.UUID;

public record TypingEventDto(
        UUID chatId,
        UUID senderId
) {
}