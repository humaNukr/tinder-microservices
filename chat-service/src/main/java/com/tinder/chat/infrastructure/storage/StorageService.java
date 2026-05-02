package com.tinder.chat.infrastructure.storage;

public interface StorageService {
    String generateTempLinkForUploading(String objectKey);

    String generateTempLinkForViewing(String objectKey);
}
