package com.tinder.chat.infrastructure.redis;

import com.tinder.chat.chat.dto.ProfileResponse;

import java.util.List;
import java.util.UUID;

public interface ProfileRedisService {
    List<ProfileResponse> getCachedProfiles(List<UUID> usersIds);

    void cacheProfiles(List<ProfileResponse> profiles);
}
