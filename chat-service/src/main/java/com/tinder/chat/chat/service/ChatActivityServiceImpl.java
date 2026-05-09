package com.tinder.chat.chat.service;

import com.tinder.chat.chat.dto.ReadReceiptEventDto;
import com.tinder.chat.chat.dto.ReadReceiptRequest;
import com.tinder.chat.chat.dto.TypingEventDto;
import com.tinder.chat.chat.port.ChatEventPublisher;
import com.tinder.chat.chat.port.ChatParticipantProvider;
import com.tinder.chat.exception.AccessDeniedException;
import com.tinder.chat.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatActivityServiceImpl implements ChatActivityService {

    private final ChatParticipantService participantService;
    private final ChatParticipantProvider participantProvider;
    private final ChatEventPublisher eventPublisher;
    private final MessageRepository messageRepository;

    @Transactional
    public void processReadReceipt(UUID readerId, ReadReceiptRequest request) {
        int updatedWatermarks = participantService.updateWatermark(request.chatId(), readerId, request.messageId());

        int updatedMessages = messageRepository.markMessagesAsRead(request.chatId(), readerId, request.messageId());

        if (updatedWatermarks == 0 && updatedMessages == 0) {
            return;
        }

        Set<UUID> participants = getParticipantsAndValidate(request.chatId(), readerId);
        UUID recipientId = getPartnerId(participants, readerId);

        ReadReceiptEventDto eventDto = new ReadReceiptEventDto(
                request.chatId(),
                readerId,
                recipientId,
                request.messageId()
        );
        eventPublisher.publishReadReceipt(eventDto);
    }

    public void processTypingEvent(TypingEventDto requestDto, UUID senderId) {
        eventPublisher.publishTypingEvent(new TypingEventDto(requestDto.chatId(), senderId));
    }

    private Set<UUID> getParticipantsAndValidate(UUID chatId, UUID userId) {
        Set<UUID> participants = participantProvider.getParticipants(chatId);
        if (!participants.contains(userId)) {
            throw new AccessDeniedException("User is not a participant of this chat");
        }
        return participants;
    }

    private UUID getPartnerId(Set<UUID> participants, UUID userId) {
        return participants.stream()
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Recipient not found"));
    }
}