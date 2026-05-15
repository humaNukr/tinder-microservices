package com.tinder.chat.infrastructure.adapter.out.persistence;

import com.tinder.chat.domain.enums.MessageContentType;
import com.tinder.chat.domain.enums.MessageStatus;
import com.tinder.chat.domain.exception.EntityNotFoundException;
import com.tinder.chat.domain.model.Chat;
import com.tinder.chat.domain.model.Message;
import com.tinder.chat.infrastructure.adapter.out.persistence.repository.MessageJpaRepository;
import com.tinder.chat.shared.dto.common.CursorPage;
import com.tinder.chat.util.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class MessagePersistenceAdapterIT extends IntegrationTestBase {

    @Autowired
    private MessagePersistenceAdapter adapter;

    @Autowired
    private MessageJpaRepository messageRepository;

    @Autowired
    private ChatPersistenceAdapter chatPersistenceAdapter;

    private UUID chatId;
    private UUID senderId;

    @BeforeEach
    void setUp() {
        this.chatId = UUID.randomUUID();

        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        // Лексикографічне порівняння для відповідності CHECK constraint у Postgres
        boolean isU1Smaller = u1.toString().compareTo(u2.toString()) < 0;
        UUID low = isU1Smaller ? u1 : u2;
        UUID high = isU1Smaller ? u2 : u1;

        this.senderId = low;

        Chat chat = Chat.builder()
                .id(chatId)
                .user1Id(low)
                .user2Id(high)
                .build();
        chatPersistenceAdapter.save(chat);
    }

    private Message saveMessage(String content, MessageStatus status) {
        Message message = Message.builder()
                .chatId(chatId)
                .senderId(senderId)
                .contentType(MessageContentType.TEXT)
                .content(content)
                .status(status)
                .build();
        return adapter.save(message);
    }

    @Nested
    class SaveAndGet {

        @Test
        void save_ValidMessage_PersistsAndReturnsWithId() {
            Message message = Message.builder()
                    .chatId(chatId)
                    .senderId(senderId)
                    .contentType(MessageContentType.TEXT)
                    .content("Hello integration test!")
                    .status(MessageStatus.SENT)
                    .build();

            Message savedMessage = adapter.save(message);

            assertNotNull(savedMessage.getId());
            assertEquals("Hello integration test!", savedMessage.getContent());
            assertTrue(messageRepository.existsById(savedMessage.getId()));
        }

        @Test
        void getById_ExistingId_ReturnsMessage() {
            Message saved = saveMessage("Test fetch", MessageStatus.SENT);

            Message fetched = adapter.getById(saved.getId());

            assertEquals(saved.getId(), fetched.getId());
        }

        @Test
        void getById_NonExistingId_ThrowsEntityNotFoundException() {
            assertThrows(EntityNotFoundException.class, () -> adapter.getById(999L));
        }
    }

    @Nested
    class GetPendingMessageByObjectKey {

        @Test
        void getPendingMessageByObjectKey_ExistingKey_ReturnsMessage() {
            String objectKey = "chats/" + chatId + "/file.jpg";
            Message saved = saveMessage(objectKey, MessageStatus.UPLOADING);

            Message fetched = adapter.getPendingMessageByObjectKey(objectKey);

            assertEquals(saved.getId(), fetched.getId());
            assertEquals(MessageStatus.UPLOADING, fetched.getStatus());
        }

        @Test
        void getPendingMessageByObjectKey_NonExistingKey_ThrowsEntityNotFoundException() {
            assertThrows(EntityNotFoundException.class, () ->
                    adapter.getPendingMessageByObjectKey("invalid-key")
            );
        }
    }

    @Nested
    class GetChatHistoryPage {

        @Test
        void getChatHistoryPage_FirstPage_ReturnsLimitItemsAndHasNext() {
            saveMessage("Msg 1", MessageStatus.SENT);
            saveMessage("Msg 2", MessageStatus.SENT);
            Message msg3 = saveMessage("Msg 3", MessageStatus.SENT);
            Message msg4 = saveMessage("Msg 4", MessageStatus.SENT);
            Message msg5 = saveMessage("Msg 5", MessageStatus.SENT);

            CursorPage<Message> page = adapter.getChatHistoryPage(chatId, null, 3);

            List<Message> data = page.data();
            assertEquals(3, data.size());
            assertEquals(msg5.getId(), data.get(0).getId());
            assertEquals(msg4.getId(), data.get(1).getId());
            assertEquals(msg3.getId(), data.get(2).getId());

            assertTrue(page.hasNext());
            assertEquals(msg3.getId(), page.nextCursor());
        }

        @Test
        void getChatHistoryPage_SecondPageWithCursor_ReturnsRemainingItemsAndNoNext() {
            Message msg1 = saveMessage("Msg 1", MessageStatus.SENT);
            Message msg2 = saveMessage("Msg 2", MessageStatus.SENT);
            Message msg3 = saveMessage("Msg 3", MessageStatus.SENT);

            CursorPage<Message> page = adapter.getChatHistoryPage(chatId, msg3.getId(), 3);

            List<Message> data = page.data();
            assertEquals(2, data.size());
            assertEquals(msg2.getId(), data.get(0).getId());
            assertEquals(msg1.getId(), data.get(1).getId());

            assertFalse(page.hasNext());
            assertEquals(msg1.getId(), page.nextCursor());
        }

        @Test
        void getChatHistoryPage_ExcludesUploadingMessages() {
            saveMessage("Uploading Msg", MessageStatus.UPLOADING);
            Message msg2 = saveMessage("Sent Msg", MessageStatus.SENT);

            CursorPage<Message> page = adapter.getChatHistoryPage(chatId, null, 10);

            assertEquals(1, page.data().size());
            assertEquals(msg2.getId(), page.data().getFirst().getId());
        }
    }
}