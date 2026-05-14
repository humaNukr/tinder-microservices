package com.tinder.chat.shared.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EditMessageRequest(
        @NotNull Long messageId,
        @NotNull UUID chatId,
        @NotBlank String newContent
) {
}