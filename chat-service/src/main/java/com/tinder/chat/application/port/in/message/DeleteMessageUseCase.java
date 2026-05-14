package com.tinder.chat.application.port.in.message;

import com.tinder.chat.shared.dto.message.MessageDeleteDto;

import java.util.UUID;

public interface DeleteMessageUseCase {
    void deleteMessage(UUID senderId, MessageDeleteDto requestDto);
}