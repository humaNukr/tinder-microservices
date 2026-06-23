package com.tinder.profile.service.interfaces;

import java.util.List;
import java.util.UUID;

public interface ProfilePhotoService {
    void addPhotosToProfile(UUID userId, List<String> photoUrls);

    List<String> removePhotosFromProfile(UUID userId, List<String> photoUrlsToRemove);

    void reorderPhotos(UUID userId, List<String> photoUrls);
}
