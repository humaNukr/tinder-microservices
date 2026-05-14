package com.tinder.chat.application.service.room;

import com.tinder.chat.application.port.out.room.ChatParticipantPort;
import com.tinder.chat.application.port.out.room.ChatPersistencePort;
import com.tinder.chat.domain.model.Chat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatCreationServiceTest {

    @Mock
    private ChatPersistencePort chatPersistencePort;
    
    @Mock
    private ChatParticipantPort chatParticipantPort;

    @InjectMocks
    private ChatCreationService chatCreationService;

    @Captor
    private ArgumentCaptor<Chat> chatCaptor;

    private UUID user1Id;
    private UUID user2Id;
    private UUID savedChatId;

    @BeforeEach
    void setUp() {
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
        savedChatId = UUID.randomUUID();
    }

    @Nested
    class CreateChat {

        @Test
        void createChat_ValidUsers_SavesChatAndParticipants() {
            Chat mockSavedChat = createMockChat();
            when(chatPersistencePort.save(any(Chat.class))).thenReturn(mockSavedChat);

            chatCreationService.createChat(user1Id, user2Id);

            verify(chatPersistencePort).save(chatCaptor.capture());
            Chat capturedChat = chatCaptor.getValue();
            
            assertNotNull(capturedChat);
            assertEquals(2, capturedChat.getParticipants().size());
            verify(chatParticipantPort).saveParticipants(savedChatId, user1Id, user2Id);
        }
    }

    private Chat createMockChat() {
        return Chat.builder()
                .id(savedChatId)
                .user1Id(user1Id)
                .user2Id(user2Id)
                .build();
    }
}