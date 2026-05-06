package com.tinder.chat.infrastructure.outbox.contract;

import com.tinder.chat.message.model.Message;

import java.util.UUID;

public record MessageSavedEvent(
        Message savedMessage,
        UUID recipientId
) {
}