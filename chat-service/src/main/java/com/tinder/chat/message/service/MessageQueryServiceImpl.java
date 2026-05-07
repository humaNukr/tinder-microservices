package com.tinder.chat.message.service;

import com.tinder.chat.chat.dto.ChatHistoryResponseDto;
import com.tinder.chat.chat.dto.ChatInitResponseDto;
import com.tinder.chat.chat.dto.CursorPage;
import com.tinder.chat.chat.port.ChatParticipantProvider;
import com.tinder.chat.chat.service.ChatParticipantService;
import com.tinder.chat.exception.AccessDeniedException;
import com.tinder.chat.infrastructure.storage.StorageService;
import com.tinder.chat.message.dto.MessageResponseDto;
import com.tinder.chat.message.mapper.MessageMapper;
import com.tinder.chat.message.model.Message;
import com.tinder.chat.user.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MessageQueryServiceImpl implements MessageQueryService {

    private final MessageService messageService;
    private final ChatParticipantProvider participantProvider;
    private final ChatParticipantService participantService;
    private final UserPresenceService userPresenceService;
    private final StorageService storageService;
    private final MessageMapper messageMapper;

    public ChatInitResponseDto initChat(UUID chatId, UUID userId, int limit) {
        Set<UUID> participants = getParticipantsAndValidate(chatId, userId);
        UUID partnerId = getPartnerId(participants, userId);

        CursorPage<Message> historyPage = messageService.getChatHistoryPage(chatId, null, limit);

        boolean isPartnerOnline = userPresenceService.isUserOnline(partnerId);
        Long partnerWatermark = participantService.getParticipantWatermark(chatId, partnerId);
        Long myWatermark = participantService.getParticipantWatermark(chatId, userId);

        List<MessageResponseDto> messageDtos = messageMapper.toResponseDtoList(historyPage.data());

        return new ChatInitResponseDto(
                messageDtos,
                isPartnerOnline,
                partnerWatermark,
                myWatermark,
                historyPage.nextCursor(),
                historyPage.hasNext()
        );
    }

    public ChatHistoryResponseDto getChatHistory(UUID chatId, UUID userId, Long cursor, int limit) {
        getParticipantsAndValidate(chatId, userId);

        CursorPage<Message> historyPage = messageService.getChatHistoryPage(chatId, cursor, limit);
        List<MessageResponseDto> messageDtos = messageMapper.toResponseDtoList(historyPage.data());

        return new ChatHistoryResponseDto(
                messageDtos,
                historyPage.nextCursor(),
                historyPage.hasNext()
        );
    }

    public String getMediaViewUrl(UUID chatId, String fileName, UUID userId) {
        getParticipantsAndValidate(chatId, userId);

        String objectKey = String.format("chats/%s/%s", chatId, fileName);
        return storageService.generateTempLinkForViewing(objectKey);
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