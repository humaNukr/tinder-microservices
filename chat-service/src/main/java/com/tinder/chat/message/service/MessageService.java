package com.tinder.chat.message.service;

import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.model.Message;

import java.util.UUID;

public interface MessageService {
    Message saveMessage(UUID senderId, UUID recipientId, ChatRequestDto requestDto);
}
