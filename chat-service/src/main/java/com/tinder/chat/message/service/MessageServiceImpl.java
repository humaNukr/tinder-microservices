package com.tinder.chat.message.service;

import com.tinder.chat.message.dto.ChatRequestDto;
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
    public Message saveMessage(UUID senderId, UUID recipientId, ChatRequestDto requestDto) {
        Message message = new Message();
        message.setChatId(requestDto.chatId());
        message.setSenderId(senderId);
        message.setContentType(requestDto.type());
        message.setContent(requestDto.payload());

        Message savedMessage = messageRepository.save(message);

        eventPublisher.publishEvent(new MessageSavedEvent(savedMessage, recipientId));

        return savedMessage;
    }
}