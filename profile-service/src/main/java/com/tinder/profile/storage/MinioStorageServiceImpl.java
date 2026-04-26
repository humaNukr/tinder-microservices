package com.tinder.profile.storage;

import com.tinder.profile.exception.storage.BucketAccessException;
import com.tinder.profile.exception.storage.FileUploadException;
import com.tinder.profile.exception.storage.StorageIOException;
import com.tinder.profile.properties.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageServiceImpl implements StorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public String upload(MultipartFile file, UUID userId) {
        String path = userId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucketName())
                            .object(path)
                            .stream(file.getInputStream(), file.getSize(), -1L)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (ErrorResponseException e) {
            throw new BucketAccessException("Bucket error: " + e.errorResponse().message(), e);
        } catch (IOException e) {
            throw new StorageIOException("I/O error while uploading file", e);
        } catch (Exception e) {
            throw new FileUploadException("Unexpected error during file upload", e);
        }

        return minioProperties.bucketName() + "/" + path;
    }


    @Override
    public void deleteFiles(List<String> fileKeys) {
        if (fileKeys == null || fileKeys.isEmpty()) {
            return;
        }

        String bucketName = minioProperties.bucketName();

        List<DeleteRequest.Object> objectsToDelete = fileKeys.stream()
                .map(fileKey -> {
                    int firstSlashIndex = fileKey.indexOf('/');
                    String objectName = fileKey.substring(firstSlashIndex + 1);
                    return new DeleteRequest.Object(objectName);
                })
                .toList();

        try {
            Iterable<Result<DeleteResult.Error>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(objectsToDelete)
                            .build()
            );

            for (Result<DeleteResult.Error> result : results) {
                DeleteResult.Error error = result.get();
                log.warn("Failed to delete file {}: {}", error.objectName(), error.message());
            }
        } catch (Exception e) {
            throw new StorageIOException("Unexpected error during bulk file deletion", e);
        }
    }


    @PostConstruct
    private void createBucketIfNotExists() {
        String bucketName = minioProperties.bucketName();

        boolean found;
        try {
            found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (MinioException e) {
            throw new BucketAccessException("Bucket error: " + e.getMessage(), e);
        }

        if (!found) {
            try {


                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                String policy = """
                            {
                              "Statement": [
                                {
                                  "Action": "s3:GetObject",
                                  "Effect": "Allow",
                                  "Principal": "*",
                                  "Resource": "arn:aws:s3:::%s/*"
                                }
                              ],
                              "Version": "2012-10-17"
                            }
                        """.formatted(bucketName);

                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build()
                );
            } catch (MinioException e) {
                throw new BucketAccessException("Bucket error: " + e.getMessage(), e);
            }
        }
    }
}
