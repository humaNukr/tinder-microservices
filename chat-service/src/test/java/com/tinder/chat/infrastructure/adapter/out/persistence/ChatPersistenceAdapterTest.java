package com.tinder.chat.infrastructure.adapter.out.persistence;

import com.tinder.chat.domain.exception.EntityNotFoundException;
import com.tinder.chat.domain.model.Chat;
import com.tinder.chat.domain.model.ChatPreview;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.ChatJpaEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.mapper.ChatEntityMapper;
import com.tinder.chat.infrastructure.adapter.out.persistence.mapper.ChatPreviewMapper;
import com.tinder.chat.infrastructure.adapter.out.persistence.projections.ChatParticipantsProjection;
import com.tinder.chat.infrastructure.adapter.out.persistence.projections.ChatPreviewProjection;
import com.tinder.chat.infrastructure.adapter.out.persistence.repository.ChatJpaRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatPersistenceAdapterTest {

    @Mock
    private ChatJpaRepository repository;
    @Mock
    private ChatPreviewMapper previewMapper;
    @Mock
    private ChatEntityMapper chatMapper;

    @InjectMocks
    private ChatPersistenceAdapter chatPersistenceAdapter;

    private UUID chatId;
    private UUID user1Id;
    private UUID user2Id;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        boolean isU1Smaller = u1.toString().compareTo(u2.toString()) < 0;
        user1Id = isU1Smaller ? u1 : u2;
        user2Id = isU1Smaller ? u2 : u1;
    }

    @Nested
    class Save {

        @Test
        void save_ValidChat_ReturnsSavedChat() {
            Chat domainChat = mock(Chat.class);
            ChatJpaEntity entityToSave = mock(ChatJpaEntity.class);
            ChatJpaEntity savedEntity = mock(ChatJpaEntity.class);
            Chat savedDomainChat = mock(Chat.class);

            when(chatMapper.toEntity(domainChat)).thenReturn(entityToSave);
            when(repository.save(entityToSave)).thenReturn(savedEntity);
            when(chatMapper.toDomain(savedEntity)).thenReturn(savedDomainChat);

            Chat result = chatPersistenceAdapter.save(domainChat);

            assertEquals(savedDomainChat, result);
            verify(chatMapper).toEntity(domainChat);
            verify(repository).save(entityToSave);
            verify(chatMapper).toDomain(savedEntity);
        }
    }

    @Nested
    class FindChatPreviewsByUserId {

        @Test
        void findChatPreviewsByUserId_ValidInput_ReturnsChatPreviews() {
            int limit = 10;
            int offset = 0;
            ChatPreviewProjection projection = mock(ChatPreviewProjection.class);
            ChatPreview domainPreview = mock(ChatPreview.class);

            when(repository.findChatPreviewsByUserId(user1Id, limit, offset)).thenReturn(List.of(projection));
            when(previewMapper.toDomain(projection)).thenReturn(domainPreview);

            List<ChatPreview> result = chatPersistenceAdapter.findChatPreviewsByUserId(user1Id, limit, offset);

            assertEquals(1, result.size());
            assertEquals(domainPreview, result.getFirst());
            verify(repository).findChatPreviewsByUserId(user1Id, limit, offset);
            verify(previewMapper).toDomain(projection);
        }
    }

    @Nested
    class GetChatParticipants {

        @Test
        void getChatParticipants_ChatExists_ReturnsParticipantsSet() {
            ChatParticipantsProjection projection = mock(ChatParticipantsProjection.class);
            when(projection.getUser1Id()).thenReturn(user1Id);
            when(projection.getUser2Id()).thenReturn(user2Id);
            when(repository.findParticipantsById(chatId)).thenReturn(Optional.of(projection));

            Set<UUID> result = chatPersistenceAdapter.getChatParticipants(chatId);

            assertEquals(2, result.size());
            assertTrue(result.contains(user1Id));
            assertTrue(result.contains(user2Id));
            verify(repository).findParticipantsById(chatId);
        }

        @Test
        void getChatParticipants_ChatDoesNotExist_ThrowsEntityNotFoundException() {
            when(repository.findParticipantsById(chatId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () ->
                    chatPersistenceAdapter.getChatParticipants(chatId)
            );
            verify(repository).findParticipantsById(chatId);
        }
    }
}