package com.tinder.chat.shared.dto.media;

import com.tinder.chat.domain.enums.MessageContentType;
import com.tinder.chat.shared.validation.ValueOfEnum;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MediaInitRequest(
        @NotNull UUID localId,
        @NotNull String fileExtension,
        @ValueOfEnum(enumClass = MessageContentType.class) String type
) {
}