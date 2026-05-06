package com.tinder.profile.contoller;

import com.tinder.profile.service.interfaces.ProfilePhotoFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles/me/photos")
@RequiredArgsConstructor
public class ProfilePhotoController {

    private final ProfilePhotoFacade profilePhotoFacade;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void uploadPhotos(@RequestPart("files") List<MultipartFile> files, @RequestHeader("X-User-Id") String userId) {
        profilePhotoFacade.uploadAndAttachPhotos(files, userId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhotos(
            @RequestBody List<String> photoUrls,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        profilePhotoFacade.deleteSpecificPhotos(photoUrls, userId);
    }
}