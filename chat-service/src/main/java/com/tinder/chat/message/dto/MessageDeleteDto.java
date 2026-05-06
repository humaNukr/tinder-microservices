package com.tinder.chat.message.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MessageDeleteDto(
        @NotNull UUID chatId,
        @NotNull Long messageId
) {
}