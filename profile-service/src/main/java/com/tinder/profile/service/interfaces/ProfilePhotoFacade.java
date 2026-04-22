package com.tinder.profile.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface ProfilePhotoFacade {
    void uploadAndAttachPhotos(List<MultipartFile> files, String userId);

    InputStream downloadPhoto(String fileKey);
}
