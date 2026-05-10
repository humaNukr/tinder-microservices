package com.tinder.chat.shared.dto.event;

import com.tinder.chat.domain.model.Message;

import java.util.UUID;

public record MessageSavedEvent(
        Message savedMessage,
        UUID recipientId
) {
}