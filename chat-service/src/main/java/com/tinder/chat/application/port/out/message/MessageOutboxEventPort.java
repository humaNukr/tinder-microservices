package com.tinder.chat.application.port.out.message;

import com.tinder.chat.domain.model.Message;

import java.util.UUID;

public interface MessageOutboxEventPort {
    void publishMessageSavedEvent(Message message, UUID recipientId);
}