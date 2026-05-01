package com.tinder.chat.chat.dto;

import com.tinder.chat.message.enums.MessageContentType;

public record MediaInitRequest(
        String fileExtension,
        MessageContentType type
) {
}