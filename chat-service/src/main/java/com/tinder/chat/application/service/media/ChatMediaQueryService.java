package com.tinder.chat.application.service.media;

import com.tinder.chat.application.port.in.media.GetMediaViewUrlQuery;
import com.tinder.chat.application.port.out.media.MediaStoragePort;
import com.tinder.chat.application.service.room.ChatRoomValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMediaQueryService implements GetMediaViewUrlQuery {

    private final ChatRoomValidator chatRoomValidator;
    private final MediaStoragePort storagePort;

    @Override
    public String getMediaViewUrl(UUID chatId, String fileName, UUID userId) {
        chatRoomValidator.validateAndGetParticipants(chatId, userId);

        String objectKey = String.format("chats/%s/%s", chatId, fileName);
        return storagePort.generateTempLinkForViewing(objectKey);
    }
}