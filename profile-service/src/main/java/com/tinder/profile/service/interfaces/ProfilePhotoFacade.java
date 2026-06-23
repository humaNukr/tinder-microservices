package com.tinder.profile.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProfilePhotoFacade {
    void uploadAndAttachPhotos(List<MultipartFile> files, UUID userId);

    void reorderPhotos(UUID userId, List<String> photoUrls);

    void deleteSpecificPhotos(List<String> photoUrls, UUID userId);
}
