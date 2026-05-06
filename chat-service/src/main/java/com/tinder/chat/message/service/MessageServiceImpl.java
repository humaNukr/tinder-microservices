package com.tinder.chat.message.service;

import com.tinder.chat.exception.EntityNotFoundException;
import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.enums.MessageContentType;
import com.tinder.chat.message.enums.MessageStatus;
import com.tinder.chat.infrastructure.outbox.contract.MessageSavedEvent;
import com.tinder.chat.message.model.Message;
import com.tinder.chat.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

        Message pendingMessage = Message.builder()
                .chatId(chatId)
                .senderId(senderId)
                .contentType(type)
                .content(objectKey)
                .status(MessageStatus.UPLOADING)
                .build();

        return messageRepository.save(pendingMessage);
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
    public List<Message> getChatHistory(UUID chatId, Long cursorId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        if (cursorId == null) {
            return messageRepository.findByChatIdAndStatusOrderByIdDesc(chatId, MessageStatus.SENT, pageable);
        } else {
            return messageRepository.findHistoryByCursor(chatId, MessageStatus.SENT, cursorId, pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Message getPendingMessageByObjectKey(String objectKey) {
        return messageRepository.findByContentAndStatus(objectKey, MessageStatus.UPLOADING)
                .orElseThrow(() -> new EntityNotFoundException("Pending message not found for key: " + objectKey));
    }
}