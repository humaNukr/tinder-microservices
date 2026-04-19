package com.tinder.profile.contoller;

import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.LocationUpdateRequest;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.service.interfaces.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @PostMapping("/onboarding")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse createProfile(@RequestBody @Valid CreateProfileRequest request,
                                         @RequestHeader("X-User-Id") String userId) {
        return profileService.createProfile(userId, request);
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public ProfileResponse getProfile(@RequestHeader("X-User-Id") String userId) {
        return profileService.getMyProfile(userId);
    }

    @PatchMapping("/me/location")
    @ResponseStatus(HttpStatus.OK)
    public void updateLocation(
            @RequestHeader("X-User-Id") String userId,
            @Valid LocationUpdateRequest request
    ) {
        profileService.updateLocation(userId, request);
    }
}
