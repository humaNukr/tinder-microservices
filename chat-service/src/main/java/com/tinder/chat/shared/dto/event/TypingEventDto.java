package com.tinder.chat.shared.dto.event;

import java.util.UUID;

public record TypingEventDto(
        UUID chatId,
        UUID senderId
) {
}