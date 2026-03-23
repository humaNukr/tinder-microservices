package com.tinder.profile.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface ProfilePhotoFacade {
    void uploadAndAttachPhotos(List<MultipartFile> files, UUID userId);

    InputStream downloadPhoto(String fileKey);
}
