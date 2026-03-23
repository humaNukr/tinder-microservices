package com.tinder.profile.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

public interface StorageService {
    String upload(MultipartFile file, UUID userId);

    InputStream downloadFile(String fileKey);

    void deleteFile(String fileKey);
}
