package com.tinder.chat.shared.mapper;

import com.tinder.chat.domain.enums.MessageContentType;
import com.tinder.chat.domain.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageContentResolverTest {

    private MessageContentResolver resolver;
    private UUID chatId;
    private UUID senderId;

    @BeforeEach
    void setUp() {
        resolver = new MessageContentResolver();
        chatId = UUID.randomUUID();
        senderId = UUID.randomUUID();
    }

    private Message createMessage(String content, MessageContentType type, boolean isDeleted) {
        Message message = Message.builder()
                .chatId(chatId)
                .senderId(senderId)
                .content(content)
                .contentType(type)
                .build();

        if (isDeleted) {
            message.deleteBy(senderId, chatId);
        }
        return message;
    }

    @Nested
    class ResolveContent {

        @Test
        void resolveContent_NullMessage_ReturnsNull() {
            String result = resolver.resolveContent(null);

            assertNull(result);
        }

        @Test
        void resolveContent_DeletedMessage_ReturnsTombstone() {
            Message message = createMessage("Original text", MessageContentType.TEXT, true);

            String result = resolver.resolveContent(message);

            assertEquals("TOMBSTONE", result);
        }

        @Test
        void resolveContent_TextMessage_ReturnsRawContent() {
            String expectedContent = "Hello World";
            Message message = createMessage(expectedContent, MessageContentType.TEXT, false);

            String result = resolver.resolveContent(message);

            assertEquals(expectedContent, result);
        }

        @Test
        void resolveContent_MediaMessage_ReturnsFormattedUrl() {
            String rawContentPath = "chats/" + chatId + "/file-uuid.jpg";
            Message message = createMessage(rawContentPath, MessageContentType.IMAGE, false);

            String result = resolver.resolveContent(message);

            assertEquals(String.format("/api/v1/chats/%s/media/file-uuid.jpg", chatId), result);
        }
    }
}