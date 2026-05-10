package com.tinder.chat.application.service.room;

import com.tinder.chat.application.port.in.room.GetChatHistoryQuery;
import com.tinder.chat.application.port.in.room.InitChatQuery;
import com.tinder.chat.application.port.out.message.MessagePersistencePort;
import com.tinder.chat.application.port.out.presence.UserPresencePort;
import com.tinder.chat.application.port.out.room.ChatParticipantPersistencePort;
import com.tinder.chat.domain.model.Message;
import com.tinder.chat.shared.dto.common.CursorPage;
import com.tinder.chat.shared.dto.message.MessageResponseDto;
import com.tinder.chat.shared.dto.room.ChatHistoryResponseDto;
import com.tinder.chat.shared.dto.room.ChatInitResponseDto;
import com.tinder.chat.shared.mapper.MessageResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatHistoryQueryService implements InitChatQuery, GetChatHistoryQuery {

    private final MessagePersistencePort persistencePort;
    private final ChatRoomValidator chatRoomValidator;
    private final ChatParticipantPersistencePort participantPersistencePort;
    private final UserPresencePort userPresencePort;
    private final MessageResponseMapper messageResponseMapper;

    @Override
    public ChatInitResponseDto initChat(UUID chatId, UUID userId, int limit) {
        Set<UUID> participants = chatRoomValidator.validateAndGetParticipants(chatId, userId);
        UUID partnerId = chatRoomValidator.getPartnerId(participants, userId);

        CursorPage<Message> historyPage = persistencePort.getChatHistoryPage(chatId, null, limit);

        boolean isPartnerOnline = userPresencePort.isUserOnline(partnerId);

        Long partnerWatermark = participantPersistencePort.findLastReadMessageId(chatId, partnerId).orElse(null);
        Long myWatermark = participantPersistencePort.findLastReadMessageId(chatId, userId).orElse(null);

        List<MessageResponseDto> messageDtos = messageResponseMapper.toResponseDtoList(historyPage.data());

        return new ChatInitResponseDto(
                messageDtos,
                isPartnerOnline,
                partnerWatermark,
                myWatermark,
                historyPage.nextCursor(),
                historyPage.hasNext()
        );
    }

    @Override
    public ChatHistoryResponseDto getChatHistory(UUID chatId, UUID userId, Long cursor, int limit) {
        chatRoomValidator.validateAndGetParticipants(chatId, userId);

        CursorPage<Message> historyPage = persistencePort.getChatHistoryPage(chatId, cursor, limit);
        List<MessageResponseDto> messageDtos = messageResponseMapper.toResponseDtoList(historyPage.data());

        return new ChatHistoryResponseDto(
                messageDtos,
                historyPage.nextCursor(),
                historyPage.hasNext()
        );
    }
}