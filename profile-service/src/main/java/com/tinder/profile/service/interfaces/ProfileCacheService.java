package com.tinder.profile.service.interfaces;

import com.tinder.profile.dto.ProfileResponse;

import java.util.Optional;
import java.util.UUID;

public interface ProfileCacheService {
    void cacheProfile(ProfileResponse profile);

    Optional<ProfileResponse> getCachedProfile(UUID userId);
}
