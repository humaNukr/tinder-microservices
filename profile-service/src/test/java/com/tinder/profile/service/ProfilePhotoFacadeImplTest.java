package com.tinder.profile.service;

import com.tinder.profile.exception.storage.FileUploadException;
import com.tinder.profile.service.impl.ProfilePhotoFacadeImpl;
import com.tinder.profile.service.interfaces.ProfileService;
import com.tinder.profile.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfilePhotoFacadeImpl")
class ProfilePhotoFacadeImplTest {

    private final UUID userId = UUID.randomUUID();
    @Mock
    private StorageService storageService;
    @Mock
    private ProfileService profileService;
    @InjectMocks
    private ProfilePhotoFacadeImpl facade;

    @Nested
    @DisplayName("uploadAndAttachPhotos()")
    class Upload {

        @Test
        @DisplayName("uploads files and attaches to profile")
        void success_UploadsAndAttaches() {
            MockMultipartFile file = new MockMultipartFile(
                    "files", "photo.jpg", "image/jpeg", "bytes".getBytes());
            when(storageService.upload(file, userId)).thenReturn("tinder-media/u/photo.jpg");

            facade.uploadAndAttachPhotos(List.of(file), userId);

            verify(profileService).addPhotosToProfile(userId, List.of("tinder-media/u/photo.jpg"));
        }

        @Test
        @DisplayName("rolls back uploaded files when profile update fails")
        void profileUpdateFails_RollsBackStorage() {
            MockMultipartFile file = new MockMultipartFile(
                    "files", "photo.jpg", "image/jpeg", "bytes".getBytes());
            when(storageService.upload(file, userId)).thenReturn("tinder-media/u/photo.jpg");
            doThrow(new RuntimeException("db error"))
                    .when(profileService).addPhotosToProfile(eq(userId), any());

            assertThrows(FileUploadException.class,
                    () -> facade.uploadAndAttachPhotos(List.of(file), userId));

            verify(storageService).deleteFiles(List.of("tinder-media/u/photo.jpg"));
        }
    }

    @Nested
    @DisplayName("deleteSpecificPhotos()")
    class DeleteSpecific {

        @Test
        @DisplayName("deletes only photos removed from profile")
        void removedFromProfile_DeletesFromStorage() {
            List<String> removed = List.of("tinder-media/u/photo.jpg");
            when(profileService.removePhotosFromProfile(userId, removed)).thenReturn(removed);

            facade.deleteSpecificPhotos(removed, userId);

            verify(storageService).deleteFiles(removed);
        }

        @Test
        @DisplayName("skips storage delete when nothing was removed")
        void nothingRemoved_SkipsStorage() {
            List<String> requested = List.of("unknown.jpg");
            when(profileService.removePhotosFromProfile(userId, requested)).thenReturn(List.of());

            facade.deleteSpecificPhotos(requested, userId);

            verify(storageService, never()).deleteFiles(any());
        }
    }

    @Nested
    @DisplayName("deletePhotos()")
    class DeletePhotos {

        @Test
        @DisplayName("delegates to storage")
        void keys_DeletesFromStorage() {
            List<String> keys = List.of("tinder-media/u/a.jpg");

            facade.deletePhotos(keys);

            verify(storageService).deleteFiles(keys);
        }
    }
}
