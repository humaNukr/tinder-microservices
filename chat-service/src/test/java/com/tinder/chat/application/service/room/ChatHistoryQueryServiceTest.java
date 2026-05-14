package com.tinder.chat.application.service.room;

import com.tinder.chat.application.port.out.message.MessagePersistencePort;
import com.tinder.chat.application.port.out.presence.UserPresencePort;
import com.tinder.chat.application.port.out.room.ChatParticipantPersistencePort;
import com.tinder.chat.domain.model.Message;
import com.tinder.chat.shared.dto.common.CursorPage;
import com.tinder.chat.shared.dto.message.MessageResponseDto;
import com.tinder.chat.shared.dto.room.ChatHistoryResponseDto;
import com.tinder.chat.shared.dto.room.ChatInitResponseDto;
import com.tinder.chat.shared.mapper.MessageResponseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatHistoryQueryServiceTest {

    @Mock
    private MessagePersistencePort persistencePort;
    @Mock
    private ChatRoomValidator chatRoomValidator;
    @Mock
    private ChatParticipantPersistencePort participantPersistencePort;
    @Mock
    private UserPresencePort userPresencePort;
    @Mock
    private MessageResponseMapper messageResponseMapper;

    @InjectMocks
    private ChatHistoryQueryService chatHistoryQueryService;

    private UUID chatId;
    private UUID userId;
    private UUID partnerId;
    private int limit;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        userId = UUID.randomUUID();
        partnerId = UUID.randomUUID();
        limit = 20;
    }

    @Nested
    class InitChat {

        @Test
        void initChat_ValidRequest_ReturnsChatInitResponseDto() {
            Set<UUID> participants = Set.of(userId, partnerId);
            when(chatRoomValidator.validateAndGetParticipants(chatId, userId)).thenReturn(participants);
            when(chatRoomValidator.getPartnerId(participants, userId)).thenReturn(partnerId);

            CursorPage<Message> mockPage = createMockPage();
            List<MessageResponseDto> mockDtos = List.of(mock(MessageResponseDto.class));

            when(persistencePort.getChatHistoryPage(chatId, null, limit)).thenReturn(mockPage);
            when(userPresencePort.isUserOnline(partnerId)).thenReturn(true);
            when(participantPersistencePort.findLastReadMessageId(chatId, partnerId)).thenReturn(Optional.of(10L));
            when(participantPersistencePort.findLastReadMessageId(chatId, userId)).thenReturn(Optional.empty());
            when(messageResponseMapper.toResponseDtoList(mockPage.data())).thenReturn(mockDtos);

            ChatInitResponseDto result = chatHistoryQueryService.initChat(chatId, userId, limit);

            assertEquals(mockDtos, result.messages());
            assertTrue(result.isPartnerOnline());
            assertEquals(10L, result.partnerLastReadMessageId());
            assertEquals(0L, result.myLastReadMessageId());
            assertEquals(mockPage.nextCursor(), result.nextCursor());
            assertEquals(mockPage.hasNext(), result.hasNext());

            verify(chatRoomValidator).validateAndGetParticipants(chatId, userId);
            verify(chatRoomValidator).getPartnerId(participants, userId);
        }
    }

    @Nested
    class GetChatHistory {

        @Test
        void getChatHistory_ValidRequest_ReturnsChatHistoryResponseDto() {
            Long cursor = 15L;
            Set<UUID> participants = Set.of(userId, partnerId);
            when(chatRoomValidator.validateAndGetParticipants(chatId, userId)).thenReturn(participants);

            CursorPage<Message> mockPage = createMockPage();
            List<MessageResponseDto> mockDtos = List.of(mock(MessageResponseDto.class));

            when(persistencePort.getChatHistoryPage(chatId, cursor, limit)).thenReturn(mockPage);
            when(messageResponseMapper.toResponseDtoList(mockPage.data())).thenReturn(mockDtos);

            ChatHistoryResponseDto result = chatHistoryQueryService.getChatHistory(chatId, userId, cursor, limit);

            assertEquals(mockDtos, result.messages());
            assertEquals(mockPage.nextCursor(), result.nextCursor());
            assertEquals(mockPage.hasNext(), result.hasNext());

            verify(chatRoomValidator).validateAndGetParticipants(chatId, userId);
        }
    }

    private CursorPage<Message> createMockPage() {
        List<Message> messages = List.of(mock(Message.class));
        return new CursorPage<>(messages, 5L, false);
    }
}