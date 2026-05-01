package com.tinder.chat.chat.dto;

import com.tinder.chat.infrastructure.validation.ValueOfEnum;
import com.tinder.chat.message.enums.MessageContentType;

public record MediaInitRequest(
        String fileExtension,
        @ValueOfEnum(enumClass = MessageContentType.class) String type
) {
}