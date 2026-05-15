package com.tinder.chat.infrastructure.adapter.out.persistence;

import com.tinder.chat.domain.exception.EntityNotFoundException;
import com.tinder.chat.domain.model.Chat;
import com.tinder.chat.domain.model.ChatPreview;
import com.tinder.chat.infrastructure.adapter.out.persistence.repository.ChatJpaRepository;
import com.tinder.chat.util.IntegrationTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class ChatPersistenceAdapterIT extends IntegrationTestBase {

    @Autowired
    private ChatPersistenceAdapter adapter;

    @Autowired
    private ChatJpaRepository chatRepository;

    @Nested
    class Save {

        @Test
        void save_ValidChat_PersistsInDatabaseAndReturnsMappedChat() {
            UUID chatId = UUID.randomUUID();
            UUID u1 = UUID.randomUUID();
            UUID u2 = UUID.randomUUID();

            boolean isU1Smaller = u1.toString().compareTo(u2.toString()) < 0;
            UUID low = isU1Smaller ? u1 : u2;
            UUID high = isU1Smaller ? u2 : u1;

            Chat chatToSave = Chat.builder()
                    .id(chatId)
                    .user1Id(low)
                    .user2Id(high)
                    .build();

            Chat savedChat = adapter.save(chatToSave);

            assertNotNull(savedChat);
            assertEquals(chatId, savedChat.getId());
            assertEquals(low, savedChat.getUser1Id());
            assertTrue(chatRepository.existsById(chatId));
        }
    }

    @Nested
    class GetChatParticipants {

        @Test
        void getChatParticipants_ExistingChat_ReturnsSetOfIdsFromDatabase() {
            UUID u1 = UUID.randomUUID();
            UUID u2 = UUID.randomUUID();

            boolean isU1Smaller = u1.toString().compareTo(u2.toString()) < 0;
            UUID low = isU1Smaller ? u1 : u2;
            UUID high = isU1Smaller ? u2 : u1;

            Chat chat = Chat.builder()
                    .id(UUID.randomUUID())
                    .user1Id(low)
                    .user2Id(high)
                    .build();

            adapter.save(chat);

            Set<UUID> participants = adapter.getChatParticipants(chat.getId());

            assertEquals(2, participants.size());
            assertTrue(participants.contains(low));
            assertTrue(participants.contains(high));
        }

        @Test
        void getChatParticipants_NonExistingChat_ThrowsEntityNotFoundException() {
            UUID randomId = UUID.randomUUID();

            assertThrows(EntityNotFoundException.class, () ->
                    adapter.getChatParticipants(randomId)
            );
        }
    }

    @Nested
    class FindChatPreviewsByUserId {

        @Test
        void findChatPreviewsByUserId_ValidUser_ExecutesQueryWithoutExceptions() {
            UUID u1 = UUID.randomUUID();
            UUID u2 = UUID.randomUUID();

            boolean isU1Smaller = u1.toString().compareTo(u2.toString()) < 0;
            UUID low = isU1Smaller ? u1 : u2;
            UUID high = isU1Smaller ? u2 : u1;

            Chat chat = Chat.builder()
                    .id(UUID.randomUUID())
                    .user1Id(low)
                    .user2Id(high)
                    .build();

            adapter.save(chat);

            List<ChatPreview> previewsForLow = adapter.findChatPreviewsByUserId(low, 10, 0);
            List<ChatPreview> previewsForHigh = adapter.findChatPreviewsByUserId(high, 10, 0);

            assertNotNull(previewsForLow);
            assertNotNull(previewsForHigh);
        }
    }
}