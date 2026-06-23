package com.tinder.profile.service.interfaces;

import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.dto.UpdatePreferencesRequest;
import com.tinder.profile.dto.UpdateProfileRequest;
import com.tinder.profile.dto.UserPreferencesResponse;

import java.util.List;
import java.util.UUID;

public interface ProfileCoreService {
    ProfileResponse createProfile(UUID userId, CreateProfileRequest request);

    ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    ProfileResponse getMyProfile(UUID userId);

    void deleteProfile(UUID userId);

    void deleteAccountData(UUID userId);

    UserPreferencesResponse getMyPreferences(UUID userId);

    UserPreferencesResponse updateMyPreferences(UUID userId, UpdatePreferencesRequest request);

    List<ProfileResponse> getBatchProfiles(List<UUID> ids);
}
