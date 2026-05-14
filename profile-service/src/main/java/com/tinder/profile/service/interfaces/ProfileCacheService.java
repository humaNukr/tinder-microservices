package com.tinder.profile.service.interfaces;

import com.tinder.profile.dto.ProfileResponse;

public interface ProfileCacheService {
    void cacheProfile(ProfileResponse profile);
}
