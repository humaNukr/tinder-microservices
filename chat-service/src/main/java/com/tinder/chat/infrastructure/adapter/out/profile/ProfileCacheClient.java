package com.tinder.chat.infrastructure.adapter.out.profile;

import com.tinder.chat.shared.dto.external.ProfileResponse;

import java.util.List;
import java.util.UUID;

interface ProfileCacheClient {
    List<ProfileResponse> getCachedProfiles(List<UUID> usersIds);

    void cacheProfiles(List<ProfileResponse> profiles);
}