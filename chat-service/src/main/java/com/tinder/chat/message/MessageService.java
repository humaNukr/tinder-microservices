package com.tinder.chat.message;

import com.tinder.chat.message.dto.ChatRequestDto;

import java.util.UUID;

public interface MessageService {
    Message saveMessage(UUID senderId, ChatRequestDto requestDto);
}
