package com.tinder.chat.message.service;

import com.tinder.chat.chat.dto.ChatHistoryResponseDto;
import com.tinder.chat.chat.dto.ChatInitResponseDto;
import com.tinder.chat.chat.dto.MediaInitRequest;
import com.tinder.chat.chat.dto.MediaInitResponse;
import com.tinder.chat.chat.dto.ReadReceiptRequest;
import com.tinder.chat.chat.dto.TypingEventDto;
import com.tinder.chat.chat.service.ChatActivityService;
import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.dto.MessageDeleteDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageFacade {

    private final MessageCommandService commandService;
    private final MessageQueryService queryService;
    private final ChatActivityService activityService;

    public void saveMessage(UUID senderId, ChatRequestDto requestDto) {
        commandService.saveMessage(senderId, requestDto);
    }

    public void deleteMessage(UUID senderId, MessageDeleteDto request) {
        commandService.deleteMessage(senderId, request);
    }

    public MediaInitResponse initMediaUpload(UUID chatId, UUID senderId, MediaInitRequest request) {
        return commandService.initMediaUpload(chatId, senderId, request);
    }

    public void confirmMediaUpload(String objectKey) {
        commandService.confirmMediaUpload(objectKey);
    }

    public ChatInitResponseDto initChat(UUID chatId, UUID userId, int limit) {
        return queryService.initChat(chatId, userId, limit);
    }

    public ChatHistoryResponseDto getChatHistory(UUID chatId, UUID userId, Long cursor, int limit) {
        return queryService.getChatHistory(chatId, userId, cursor, limit);
    }

    public String getMediaViewUrl(UUID chatId, String fileName, UUID userId) {
        return queryService.getMediaViewUrl(chatId, fileName, userId);
    }

    public void processReadReceipt(UUID readerId, ReadReceiptRequest request) {
        activityService.processReadReceipt(readerId, request);
    }

    public void processTypingEvent(TypingEventDto requestDto, UUID senderId) {
        activityService.processTypingEvent(requestDto, senderId);
    }
}