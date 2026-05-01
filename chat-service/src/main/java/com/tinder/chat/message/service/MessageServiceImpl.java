package com.tinder.chat.message.service;

import com.tinder.chat.exception.EntityNotFoundException;
import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.enums.MessageContentType;
import com.tinder.chat.message.enums.MessageStatus;
import com.tinder.chat.message.event.MessageSavedEvent;
import com.tinder.chat.message.model.Message;
import com.tinder.chat.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Message saveReadyMessage(UUID senderId, UUID recipientId, ChatRequestDto requestDto) {
        MessageContentType contentType = MessageContentType.valueOf(requestDto.type().toUpperCase());
        Message message = Message.builder()
                .chatId(requestDto.chatId())
                .senderId(senderId)
                .contentType(contentType)
                .content(requestDto.payload())
                .status(MessageStatus.SENT)
                .build();

        Message savedMessage = messageRepository.save(message);

        eventPublisher.publishEvent(new MessageSavedEvent(savedMessage, recipientId));

        return savedMessage;
    }

    @Override
    @Transactional
    public Message savePendingMessage(UUID chatId, UUID senderId, MessageContentType type, String objectKey) {
        log.info("--- DEBUG STEP 3 [SERVICE] ---");
        log.info("Building entity. ChatId: {}, SenderId: {}, Type: {}", chatId, senderId, type);

        Message pendingMessage = Message.builder()
                .chatId(chatId)
                .senderId(senderId)
                .contentType(type)
                .content(objectKey)
                .status(MessageStatus.UPLOADING)
                .build();

        log.info("Entity built. SenderId inside entity BEFORE save: {}", pendingMessage.getSenderId());

        Message savedMessage = messageRepository.save(pendingMessage);

        log.info("Entity SAVED! ID in DB: {}, SenderId returned from DB: {}", savedMessage.getId(), savedMessage.getSenderId());

        return savedMessage;
    }

    @Override
    @Transactional
    public Message markMessageAsSentAndPublishOutbox(Message message, UUID recipientId) {
        message.setStatus(MessageStatus.SENT);
        Message savedMessage = messageRepository.save(message);

        eventPublisher.publishEvent(new MessageSavedEvent(savedMessage, recipientId));

        return savedMessage;
    }

    @Override
    @Transactional(readOnly = true)
    public Message getMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));
    }

    @Override
    @Transactional(readOnly = true)
    public Message getPendingMessageByObjectKey(String objectKey) {
        return messageRepository.findByContentAndStatus(objectKey, MessageStatus.UPLOADING)
                .orElseThrow(() -> new EntityNotFoundException("Pending message not found for key: " + objectKey));
    }
}