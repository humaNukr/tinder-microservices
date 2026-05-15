package com.tinder.chat.application.service.media;

import com.tinder.chat.application.port.out.media.MediaStoragePort;
import com.tinder.chat.application.service.room.ChatRoomValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMediaQueryServiceTest {

    @Mock
    private ChatRoomValidator chatRoomValidator;

    @Mock
    private MediaStoragePort storagePort;

    @InjectMocks
    private ChatMediaQueryService chatMediaQueryService;

    private UUID chatId;
    private UUID userId;
    private String fileName;
    private String expectedObjectKey;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        userId = UUID.randomUUID();
        fileName = "image.jpg";
        expectedObjectKey = String.format("chats/%s/%s", chatId, fileName);
    }

    private void setupValidator() {
        when(chatRoomValidator.validateAndGetParticipants(chatId, userId)).thenReturn(Set.of(userId, UUID.randomUUID()));
    }

    @Nested
    class GetMediaViewUrl {

        @Test
        void getMediaViewUrl_ValidRequest_ReturnsGeneratedUrl() {
            String expectedUrl = "https://storage.com/temp-url";
            setupValidator();
            when(storagePort.generateTempLinkForViewing(expectedObjectKey)).thenReturn(expectedUrl);

            String actualUrl = chatMediaQueryService.getMediaViewUrl(chatId, fileName, userId);

            assertEquals(expectedUrl, actualUrl);
            verify(chatRoomValidator).validateAndGetParticipants(chatId, userId);
            verify(storagePort).generateTempLinkForViewing(expectedObjectKey);
        }
    }
}