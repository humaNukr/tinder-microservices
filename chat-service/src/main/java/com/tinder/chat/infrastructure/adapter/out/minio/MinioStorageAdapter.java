package com.tinder.chat.infrastructure.adapter.out.minio;

import com.tinder.chat.application.port.out.media.MediaStoragePort;
import com.tinder.chat.domain.exception.StorageException;
import com.tinder.chat.infrastructure.config.properties.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class MinioStorageAdapter implements MediaStoragePort {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public String generateTempLinkForUploading(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.PUT)
                            .bucket(minioProperties.bucketName())
                            .object(objectKey)
                            .expiry(15, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating upload link for object: {}", objectKey, e);
            throw new StorageException("Failed to generate MinIO presigned URL", e);
        }
    }

    @Override
    public String generateTempLinkForViewing(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(minioProperties.bucketName())
                            .object(objectKey)
                            .expiry(30, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating view link for object: {}", objectKey, e);
            throw new StorageException("Could not generate view link", e);
        }
    }

    @PostConstruct
    public void createBucketIfNotExists() {
        String bucketName = minioProperties.bucketName();
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Successfully created private MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket", e);
            throw new StorageException("Could not initialize storage bucket", e);
        }
    }
}