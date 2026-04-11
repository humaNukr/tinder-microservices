package com.tinder.chat.message.service;

import com.tinder.chat.chat.port.ChatParticipantProvider;
import com.tinder.chat.message.repository.MessageRepository;
import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.event.MessageSavedEvent;
import com.tinder.chat.message.model.Message;
import com.tinder.chat.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ChatParticipantProvider chatParticipantProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Message saveMessage(UUID senderId, ChatRequestDto requestDto) {
        UUID recipientId = getRecipientId(requestDto.chatId(), senderId);

        Message message = new Message();
        message.setChatId(requestDto.chatId());
        message.setSenderId(senderId);
        message.setContentType(requestDto.type());
        message.setContent(requestDto.payload());

        Message savedMessage = messageRepository.save(message);

        eventPublisher.publishEvent(new MessageSavedEvent(savedMessage, recipientId));

        return savedMessage;
    }

    private UUID getRecipientId(UUID chatId, UUID senderId) {
        Set<UUID> participants = chatParticipantProvider.getParticipants(chatId);

        if (!participants.contains(senderId)) {
            throw new AccessDeniedException("You are not a participant of this chat");
        }

        return participants.stream()
                .filter(id -> !id.equals(senderId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Chat has no second participant"));
    }
}