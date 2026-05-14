package com.tinder.chat.shared.dto.message;

import com.tinder.chat.domain.enums.MessageContentType;
import com.tinder.chat.shared.validation.ValueOfEnum;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChatRequestDto(
        @NotNull UUID localId,
        @NotNull UUID chatId,
        @NotNull String payload,
        @NotNull @ValueOfEnum(enumClass = MessageContentType.class) String type,
        Long replyToMessageId
) {
}
