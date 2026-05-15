package com.tinder.chat.infrastructure.adapter.out.minio;

import com.tinder.chat.domain.exception.StorageException;
import com.tinder.chat.infrastructure.config.properties.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioStorageAdapterTest {

    private final String bucketName = "chat-media";
    private final String objectKey = "chats/uuid/image.jpg";
    @Mock
    private MinioClient minioClient;
    @Mock
    private MinioProperties minioProperties;
    @InjectMocks
    private MinioStorageAdapter adapter;
    @Captor
    private ArgumentCaptor<GetPresignedObjectUrlArgs> urlArgsCaptor;
    @Captor
    private ArgumentCaptor<BucketExistsArgs> existsArgsCaptor;
    @Captor
    private ArgumentCaptor<MakeBucketArgs> makeArgsCaptor;

    @BeforeEach
    void setUp() {
        when(minioProperties.bucketName()).thenReturn(bucketName);
    }

    private void assertUrlArgs(GetPresignedObjectUrlArgs args, Http.Method expectedMethod, int expectedExpiryMinutes) {
        assertEquals(expectedMethod, args.method());
        assertEquals(bucketName, args.bucket());
        assertEquals(objectKey, args.object());
        assertEquals(TimeUnit.MINUTES.toSeconds(expectedExpiryMinutes), args.expiry());
    }

    @Nested
    class GenerateTempLinkForUploading {

        @Test
        void generateTempLinkForUploading_ValidRequest_ReturnsPresignedUrl() throws Exception {
            String expectedUrl = "https://minio.com/upload-link";
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(expectedUrl);

            String actualUrl = adapter.generateTempLinkForUploading(objectKey);

            assertEquals(expectedUrl, actualUrl);
            verify(minioClient).getPresignedObjectUrl(urlArgsCaptor.capture());
            GetPresignedObjectUrlArgs capturedArgs = urlArgsCaptor.getValue();
            assertUrlArgs(capturedArgs, Http.Method.PUT, 15);
        }

        @Test
        void generateTempLinkForUploading_ClientThrowsException_ThrowsStorageException() throws Exception {
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenThrow(new MinioException("Minio error"));

            assertThrows(StorageException.class, () -> adapter.generateTempLinkForUploading(objectKey));
        }
    }

    @Nested
    class GenerateTempLinkForViewing {

        @Test
        void generateTempLinkForViewing_ValidRequest_ReturnsPresignedUrl() throws Exception {
            String expectedUrl = "https://minio.com/view-link";
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(expectedUrl);

            String actualUrl = adapter.generateTempLinkForViewing(objectKey);

            assertEquals(expectedUrl, actualUrl);
            verify(minioClient).getPresignedObjectUrl(urlArgsCaptor.capture());
            GetPresignedObjectUrlArgs capturedArgs = urlArgsCaptor.getValue();
            assertUrlArgs(capturedArgs, Http.Method.GET, 30);
        }

        @Test
        void generateTempLinkForViewing_ClientThrowsException_ThrowsStorageException() throws Exception {
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenThrow(new MinioException("Minio error"));

            assertThrows(StorageException.class, () -> adapter.generateTempLinkForViewing(objectKey));
        }
    }

    @Nested
    class CreateBucketIfNotExists {

        @Test
        void createBucketIfNotExists_BucketExists_DoesNothing() throws Exception {
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            adapter.createBucketIfNotExists();

            verify(minioClient).bucketExists(existsArgsCaptor.capture());
            assertEquals(bucketName, existsArgsCaptor.getValue().bucket());
            verify(minioClient, never()).makeBucket(any());
        }

        @Test
        void createBucketIfNotExists_BucketDoesNotExist_CreatesBucket() throws Exception {
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

            adapter.createBucketIfNotExists();

            verify(minioClient).bucketExists(any());
            verify(minioClient).makeBucket(makeArgsCaptor.capture());
            assertEquals(bucketName, makeArgsCaptor.getValue().bucket());
        }

        @Test
        void createBucketIfNotExists_ClientThrowsException_ThrowsStorageException() throws Exception {
            when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                    .thenThrow(new MinioException("Connection refused"));

            assertThrows(StorageException.class, () -> adapter.createBucketIfNotExists());
        }
    }
}