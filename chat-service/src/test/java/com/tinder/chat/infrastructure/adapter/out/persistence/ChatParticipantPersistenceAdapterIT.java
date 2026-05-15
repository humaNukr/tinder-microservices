package com.tinder.chat.infrastructure.adapter.out.persistence;

import com.tinder.chat.domain.model.Chat;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.ChatJpaEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.ChatParticipantId;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.ChatParticipantJpaEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.repository.ChatJpaRepository;
import com.tinder.chat.infrastructure.adapter.out.persistence.repository.ChatParticipantJpaRepository;
import com.tinder.chat.util.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class ChatParticipantPersistenceAdapterIT extends IntegrationTestBase {

    @Autowired
    private ChatParticipantPersistenceAdapter adapter;

    @Autowired
    private ChatParticipantJpaRepository participantRepository;

    @Autowired
    private ChatJpaRepository chatRepository;

    @Autowired
    private ChatPersistenceAdapter chatPersistenceAdapter;

    private UUID chatId;
    private UUID userId;
    private ChatParticipantId participantId;

    @BeforeEach
    void setUp() {
        participantRepository.deleteAll();
        chatRepository.deleteAll();

        this.chatId = UUID.randomUUID();

        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        boolean isU1Smaller = u1.toString().compareTo(u2.toString()) < 0;
        UUID low = isU1Smaller ? u1 : u2;
        UUID high = isU1Smaller ? u2 : u1;

        this.userId = low;
        this.participantId = new ChatParticipantId(chatId, low);

        Chat chat = Chat.builder()
                .id(chatId)
                .user1Id(low)
                .user2Id(high)
                .build();

        chatPersistenceAdapter.save(chat);
    }

    private void saveParticipantWithWatermark(Long watermark) {
        ChatJpaEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found in DB"));

        ChatParticipantJpaEntity participant = new ChatParticipantJpaEntity();
        participant.setId(participantId);
        participant.setChat(chatEntity);
        participant.setLastReadMessageId(watermark);

        participantRepository.save(participant);
    }

    @Nested
    class FindLastReadMessageId {

        @Test
        void findLastReadMessageId_ParticipantExists_ReturnsWatermark() {
            Long expectedMessageId = 150L;
            saveParticipantWithWatermark(expectedMessageId);

            Optional<Long> result = adapter.findLastReadMessageId(chatId, userId);

            assertTrue(result.isPresent());
            assertEquals(expectedMessageId, result.get());
        }

        @Test
        void findLastReadMessageId_ParticipantDoesNotExist_ReturnsEmpty() {
            Optional<Long> result = adapter.findLastReadMessageId(UUID.randomUUID(), UUID.randomUUID());

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class UpdateLastReadMessageIdIfGreater {

        @Test
        void updateLastReadMessageIdIfGreater_NewIdIsGreater_UpdatesAndReturnsOne() {
            saveParticipantWithWatermark(100L);
            Long newMessageId = 150L;

            int updatedRows = adapter.updateLastReadMessageIdIfGreater(chatId, userId, newMessageId);

            assertEquals(1, updatedRows);

            Optional<Long> actualMessageId = adapter.findLastReadMessageId(chatId, userId);
            assertTrue(actualMessageId.isPresent());
            assertEquals(newMessageId, actualMessageId.get());
        }

        @Test
        void updateLastReadMessageIdIfGreater_NewIdIsSmaller_DoesNotUpdateAndReturnsZero() {
            saveParticipantWithWatermark(200L);
            Long newMessageId = 150L;

            int updatedRows = adapter.updateLastReadMessageIdIfGreater(chatId, userId, newMessageId);

            assertEquals(0, updatedRows);

            Optional<Long> actualMessageId = adapter.findLastReadMessageId(chatId, userId);
            assertTrue(actualMessageId.isPresent());
            assertEquals(200L, actualMessageId.get());
        }

        @Test
        void updateLastReadMessageIdIfGreater_ParticipantDoesNotExist_ReturnsZero() {
            int updatedRows = adapter.updateLastReadMessageIdIfGreater(UUID.randomUUID(), UUID.randomUUID(), 100L);

            assertEquals(0, updatedRows);
        }
    }
}