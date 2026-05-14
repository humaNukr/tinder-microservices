package com.tinder.chat.application.port.in.message;

import com.tinder.chat.shared.dto.message.ChatRequestDto;

import java.util.UUID;

public interface SendMessageUseCase {
    void saveMessage(UUID senderId, ChatRequestDto requestDto);
}