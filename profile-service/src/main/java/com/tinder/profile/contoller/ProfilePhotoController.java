package com.tinder.profile.contoller;

import com.tinder.profile.service.interfaces.ProfilePhotoFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
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
}