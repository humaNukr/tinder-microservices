package com.tinder.chat.application.port.in.media;

import java.util.UUID;

public interface GetMediaViewUrlQuery {
    String getMediaViewUrl(UUID chatId, String fileName, UUID userId);
}