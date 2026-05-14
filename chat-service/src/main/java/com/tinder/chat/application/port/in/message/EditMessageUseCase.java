package com.tinder.chat.application.port.in.message;

import com.tinder.chat.shared.dto.message.EditMessageRequest;

import java.util.UUID;

public interface EditMessageUseCase {
    void editMessage(UUID senderId, EditMessageRequest requestDto);
}