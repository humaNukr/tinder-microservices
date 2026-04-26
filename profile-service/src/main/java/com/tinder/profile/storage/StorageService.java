package com.tinder.profile.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface StorageService {
    String upload(MultipartFile file, UUID userId);

    void deleteFiles(List<String> fileKeys);
}
