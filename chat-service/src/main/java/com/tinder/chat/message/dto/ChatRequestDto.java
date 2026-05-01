package com.tinder.chat.message.dto;

import com.tinder.chat.infrastructure.validation.ValueOfEnum;
import com.tinder.chat.message.enums.MessageContentType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChatRequestDto(
        @NotNull UUID chatId,
        @NotNull String payload,
        @ValueOfEnum(enumClass = MessageContentType.class) String type
) {
}
