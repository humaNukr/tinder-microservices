package com.tinder.chat.application.port.out.media;

import java.util.List;

public interface MediaStoragePort {
    String generateTempLinkForUploading(String objectKey);

    String generateTempLinkForViewing(String objectKey);

    void deleteObjects(List<String> objectKeys);
}
