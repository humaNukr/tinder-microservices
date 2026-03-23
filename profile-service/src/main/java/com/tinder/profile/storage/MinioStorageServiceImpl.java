package com.tinder.profile.storage;

import com.tinder.profile.exception.storage.BucketAccessException;
import com.tinder.profile.exception.storage.FileUploadException;
import com.tinder.profile.exception.storage.StorageIOException;
import com.tinder.profile.properties.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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
    public InputStream downloadFile(String fileKey) {
        int firstSlashIndex = fileKey.indexOf('/');
        String bucketName = fileKey.substring(0, firstSlashIndex);
        String objectName = fileKey.substring(firstSlashIndex + 1);

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageIOException("Unexpected error while downloading file " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileKey) {
        int firstSlashIndex = fileKey.indexOf('/');
        String bucketName = fileKey.substring(0, firstSlashIndex);
        String objectName = fileKey.substring(firstSlashIndex + 1);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (MinioException e) {
            throw new StorageIOException("Error while deleting file " + e.getMessage(), e);
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
