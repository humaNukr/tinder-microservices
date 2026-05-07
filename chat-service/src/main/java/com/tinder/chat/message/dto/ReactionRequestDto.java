package com.tinder.chat.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReactionRequestDto(
        @NotNull UUID chatId,
        @NotNull Long messageId,
        @NotBlank @Size(max = 50) String reaction
) {
}