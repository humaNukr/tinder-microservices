package com.tinder.chat.infrastructure.adapter.out.minio;

import com.tinder.chat.application.port.out.media.MediaStoragePort;
import com.tinder.chat.domain.exception.StorageException;
import com.tinder.chat.infrastructure.config.properties.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
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

    @Override
    public void deleteObjects(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }

        List<DeleteRequest.Object> objectsToDelete =
                objectKeys.stream().map(DeleteRequest.Object::new).toList();

        try {
            Iterable<Result<DeleteResult.Error>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(minioProperties.bucketName())
                            .objects(objectsToDelete)
                            .build());

            for (Result<DeleteResult.Error> result : results) {
                DeleteResult.Error error = result.get();
                log.warn("Failed to delete chat media {}: {}", error.objectName(), error.message());
            }
            log.info("Deleted {} chat media object(s) from MinIO", objectKeys.size());
        } catch (Exception e) {
            log.error("Bulk deletion of chat media failed", e);
            throw new StorageException("Failed to delete chat media from storage", e);
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