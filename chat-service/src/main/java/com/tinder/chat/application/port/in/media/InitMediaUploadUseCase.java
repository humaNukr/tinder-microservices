package com.tinder.chat.application.port.in.media;

import com.tinder.chat.shared.dto.media.MediaInitRequest;
import com.tinder.chat.shared.dto.media.MediaInitResponse;

import java.util.UUID;

public interface InitMediaUploadUseCase {
    MediaInitResponse initMediaUpload(UUID chatId, UUID senderId, MediaInitRequest request);
}