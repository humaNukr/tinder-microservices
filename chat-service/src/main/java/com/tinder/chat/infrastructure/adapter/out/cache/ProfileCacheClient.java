package com.tinder.chat.infrastructure.adapter.out.cache;

import com.tinder.chat.shared.dto.external.ProfileResponse;

import java.util.List;
import java.util.UUID;

public interface ProfileCacheClient {
    List<ProfileResponse> getCachedProfiles(List<UUID> usersIds);

    void cacheProfiles(List<ProfileResponse> profiles);
}