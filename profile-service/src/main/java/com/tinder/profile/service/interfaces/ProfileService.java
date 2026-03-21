package com.tinder.profile.service.interfaces;

import com.tinder.profile.domain.Profile;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.ProfileResponse;

public interface ProfileService {
    ProfileResponse createProfile(CreateProfileRequest request);
}
