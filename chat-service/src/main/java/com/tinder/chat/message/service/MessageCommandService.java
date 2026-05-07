package com.tinder.chat.message.service;

import com.tinder.chat.chat.dto.MediaInitRequest;
import com.tinder.chat.chat.dto.MediaInitResponse;
import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.dto.MessageDeleteDto;

import java.util.UUID;

public interface MessageCommandService {

    void saveMessage(UUID senderId, ChatRequestDto requestDto);

    void deleteMessage(UUID senderId, MessageDeleteDto request);

    MediaInitResponse initMediaUpload(UUID chatId, UUID senderId, MediaInitRequest request);

    void confirmMediaUpload(String objectKey);
}
