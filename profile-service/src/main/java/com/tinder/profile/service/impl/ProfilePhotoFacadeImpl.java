package com.tinder.profile.service.impl;

import com.tinder.profile.exception.storage.FileUploadException;
import com.tinder.profile.service.interfaces.ProfilePhotoFacade;
import com.tinder.profile.service.interfaces.ProfileService;
import com.tinder.profile.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    public void uploadAndAttachPhotos(List<MultipartFile> files, String userId) {
        List<String> uploadedKeys = new ArrayList<>();
        UUID userIdUUID = UUID.fromString(userId);

        try {
            for (MultipartFile file : files) {
                String fileKey = storageService.upload(file, userIdUUID);
                uploadedKeys.add(fileKey);
            }

            profileService.addPhotosToProfile(userIdUUID, uploadedKeys);

        } catch (Exception e) {
            log.error("Error during photo upload. Rolling back saved files in MinIO...", e);
            storageService.deleteFiles(uploadedKeys);

            throw new FileUploadException("Failed to upload photos", e);
        }
    }

    @Override
    public void deletePhotos(List<String> photoKeys) {
        storageService.deleteFiles(photoKeys);
    }
}