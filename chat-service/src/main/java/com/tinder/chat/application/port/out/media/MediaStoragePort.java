package com.tinder.chat.application.port.out.media;

public interface MediaStoragePort {
    String generateTempLinkForUploading(String objectKey);

    String generateTempLinkForViewing(String objectKey);
}
