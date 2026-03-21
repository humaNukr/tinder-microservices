package com.tinder.profile.contoller;

import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.service.interfaces.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ProfileResponse createProfile(@RequestBody CreateProfileRequest request) {
        return profileService.createProfile(request);
    }
}
