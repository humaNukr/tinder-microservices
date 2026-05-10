package com.tinder.chat.shared.dto.message;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MessageDeleteDto(
        @NotNull UUID chatId,
        @NotNull Long messageId
) {
}