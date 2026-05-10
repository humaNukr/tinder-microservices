package com.tinder.chat.application.service.activity;

import com.tinder.chat.application.port.in.activity.ProcessReadReceiptUseCase;
import com.tinder.chat.application.port.in.activity.SendTypingEventUseCase;
import com.tinder.chat.application.port.out.notification.ChatEventPort;
import com.tinder.chat.application.port.out.room.ChatParticipantPersistencePort;
import com.tinder.chat.application.service.room.ChatRoomValidator;
import com.tinder.chat.shared.dto.activity.ReadReceiptRequest;
import com.tinder.chat.shared.dto.event.ReadReceiptEventDto;
import com.tinder.chat.shared.dto.event.TypingEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatActivityService implements ProcessReadReceiptUseCase, SendTypingEventUseCase {

    private final ChatParticipantPersistencePort persistencePort;
    private final ChatRoomValidator chatRoomValidator;
    private final ChatEventPort eventPort;

    @Override
    @Transactional
    public void processReadReceipt(UUID readerId, ReadReceiptRequest request) {
        int updatedWatermarks = persistencePort.updateLastReadMessageIdIfGreater(
                request.chatId(), readerId, request.messageId());

        if (updatedWatermarks == 0) {
            return;
        }

        Set<UUID> participants = chatRoomValidator.validateAndGetParticipants(request.chatId(), readerId);
        UUID recipientId = chatRoomValidator.getPartnerId(participants, readerId);

        ReadReceiptEventDto eventDto = new ReadReceiptEventDto(
                request.chatId(), readerId, recipientId, request.messageId());

        eventPort.publishReadReceipt(eventDto);
    }

    @Override
    public void processTypingEvent(TypingEventDto requestDto, UUID senderId) {
        eventPort.publishTypingEvent(requestDto);
    }
}