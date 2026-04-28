package com.tinder.profile.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProfilePhotoFacade {
    void uploadAndAttachPhotos(List<MultipartFile> files, String userId);

    void deletePhotos(List<String> photoKeys);

    void deleteSpecificPhotos(List<String> photoUrls, UUID userId);
}
