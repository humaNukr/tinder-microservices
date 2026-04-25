package com.tinder.profile.service.interfaces;

import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.LocationUpdateRequest;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.dto.UpdatePreferencesRequest;
import com.tinder.profile.dto.UpdateProfileRequest;
import com.tinder.profile.dto.UserPreferencesResponse;

import java.util.List;
import java.util.UUID;

public interface
ProfileService {
    ProfileResponse createProfile(String userId, CreateProfileRequest request);

    ProfileResponse updateProfile(String userId, UpdateProfileRequest request);

    ProfileResponse getMyProfile(String userId);

    UserPreferencesResponse getMyPreferences(String userId);

    UserPreferencesResponse updateMyPreferences(String userId, UpdatePreferencesRequest request);

    void addPhotosToProfile(UUID userId, List<String> photoUrls);

    void updateLocation(String userId, LocationUpdateRequest request);

    List<UUID> getCandidatesForFeed(UUID userId, int limit);

    List<ProfileResponse> getBatchProfiles(List<UUID> ids);
}
