package com.tinder.profile.service.interfaces;

import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.ProfileResponse;

import java.util.List;
import java.util.UUID;

public interface ProfileService {
    ProfileResponse createProfile(CreateProfileRequest request);

    ProfileResponse getMyProfile();

    void addPhotosToProfile(UUID userId, List<String> photoUrls);
}
