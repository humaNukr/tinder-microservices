package com.tinder.chat.message.service;

import com.tinder.chat.chat.dto.MediaInitRequest;
import com.tinder.chat.chat.dto.MediaInitResponse;
import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.dto.EditMessageRequest;
import com.tinder.chat.message.dto.MessageDeleteDto;
import com.tinder.chat.message.dto.ReactionRequestDto;

import java.util.UUID;

public interface MessageCommandService {

    void saveMessage(UUID senderId, ChatRequestDto requestDto);

    void editMessage(UUID senderId, EditMessageRequest requestDto);

    void deleteMessage(UUID senderId, MessageDeleteDto requestDto);

    MediaInitResponse initMediaUpload(UUID chatId, UUID senderId, MediaInitRequest requestDto);

    void confirmMediaUpload(String objectKey);

    void toggleReaction(UUID senderId, ReactionRequestDto requestDto);
}
