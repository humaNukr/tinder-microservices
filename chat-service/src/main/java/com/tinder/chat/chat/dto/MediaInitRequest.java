package com.tinder.chat.chat.dto;

import com.tinder.chat.infrastructure.validation.ValueOfEnum;
import com.tinder.chat.message.enums.MessageContentType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MediaInitRequest(
        @NotNull UUID localId,
        @NotNull String fileExtension,
        @ValueOfEnum(enumClass = MessageContentType.class) String type
) {
}