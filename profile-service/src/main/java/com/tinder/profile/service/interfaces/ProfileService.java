package com.tinder.profile.service.interfaces;

import com.tinder.profile.domain.Profile;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.LocationUpdateRequest;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.dto.UpdatePreferencesRequest;
import com.tinder.profile.dto.UpdateProfileRequest;
import com.tinder.profile.dto.UserPreferencesResponse;

import java.util.List;
import java.util.UUID;

public interface ProfileService {
    ProfileResponse createProfile(UUID userId, CreateProfileRequest request);

    ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    ProfileResponse getMyProfile(UUID userId);

    void deleteProfile(UUID userId);

    UserPreferencesResponse getMyPreferences(UUID userId);

    UserPreferencesResponse updateMyPreferences(UUID userId, UpdatePreferencesRequest request);

    void addPhotosToProfile(UUID userId, List<String> photoUrls);

    void updateLocation(UUID userId, LocationUpdateRequest request);

    List<UUID> getCandidatesForFeed(UUID userId, int limit);

    List<ProfileResponse> getBatchProfiles(List<UUID> ids);

    Profile getProfileEntity(UUID userId);
}
