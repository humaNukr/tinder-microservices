package com.tinder.profile.service.interfaces;

import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.LocationUpdateRequest;
import com.tinder.profile.dto.ProfileResponse;

import java.util.List;
import java.util.UUID;

public interface ProfileService {
    ProfileResponse createProfile(String userId, CreateProfileRequest request);

    ProfileResponse getMyProfile(String userId);

    void addPhotosToProfile(UUID userId, List<String> photoUrls);

    void updateLocation(String userId, LocationUpdateRequest request);
}
