package com.tinder.profile.contoller;

import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.service.interfaces.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/profiles")
@RequiredArgsConstructor
public class InternalProfileController {

    private final ProfileService profileService;

    @GetMapping("/candidates")
    @ResponseStatus(HttpStatus.OK)
    public List<UUID> getCandidatesForUser(@RequestParam UUID userId) {
        return profileService.getCandidatesForFeed(userId);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.OK)
    public List<ProfileResponse> getBatchProfiles(@RequestBody List<UUID> ids) {
        return profileService.getBatchProfiles(ids);
    }
}