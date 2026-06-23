package com.tinder.profile.service.impl;

import com.tinder.profile.exception.storage.FileUploadException;
import com.tinder.profile.service.interfaces.ProfilePhotoFacade;
import com.tinder.profile.service.interfaces.ProfilePhotoService;
import com.tinder.profile.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilePhotoFacadeImpl implements ProfilePhotoFacade {

    private final StorageService storageService;
    private final ProfilePhotoService profilePhotoService;

    @Override
    public void uploadAndAttachPhotos(List<MultipartFile> files, UUID userId) {
        List<String> uploadedKeys = Collections.synchronizedList(new ArrayList<>());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = files.stream()
                    .map(file -> CompletableFuture.supplyAsync(() -> storageService.upload(file, userId), executor)
                            .thenAccept(uploadedKeys::add))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            profilePhotoService.addPhotosToProfile(userId, uploadedKeys);

        } catch (Exception e) {
            log.error("Error during photo upload. Rolling back saved files in MinIO...", e);
            if (!uploadedKeys.isEmpty()) {
                storageService.deleteFiles(new ArrayList<>(uploadedKeys));
            }
            throw new FileUploadException("Failed to upload photos", e);
        }
    }

    @Override
    public void reorderPhotos(UUID userId, List<String> photoUrls) {
        profilePhotoService.reorderPhotos(userId, photoUrls);
    }

    private void deletePhotos(List<String> photoKeys) {
        storageService.deleteFiles(photoKeys);
    }

    @Override
    public void deleteSpecificPhotos(List<String> photoUrls, UUID userId) {
        List<String> removedPhotos = profilePhotoService.removePhotosFromProfile(userId, photoUrls);

        if (!removedPhotos.isEmpty()) {
            deletePhotos(removedPhotos);
        }
    }
}