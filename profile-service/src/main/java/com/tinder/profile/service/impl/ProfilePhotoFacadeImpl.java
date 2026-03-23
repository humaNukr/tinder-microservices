package com.tinder.profile.service.impl;

import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.exception.storage.FileUploadException;
import com.tinder.profile.service.interfaces.ProfilePhotoFacade;
import com.tinder.profile.service.interfaces.ProfileService;
import com.tinder.profile.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilePhotoFacadeImpl implements ProfilePhotoFacade {

    private final StorageService storageService;
    private final ProfileService profileService;

    @Override
    public void uploadAndAttachPhotos(List<MultipartFile> files, UUID userId) {
        List<String> uploadedKeys = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                String fileKey = storageService.upload(file, userId);
                uploadedKeys.add(fileKey);
            }

            profileService.addPhotosToProfile(userId, uploadedKeys);

        } catch (Exception e) {
            log.error("Error during photo upload. Rolling back saved files in MinIO...", e);
            uploadedKeys.forEach(storageService::deleteFile);

            throw new FileUploadException("Failed to upload photos", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream downloadPhoto(String fileKey) {
        return storageService.downloadFile(fileKey);
    }
}