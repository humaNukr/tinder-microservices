package com.tinder.feed.service.interfaces;

import com.tinder.feed.dto.ProfileResponse;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProfileProvider {

    List<UUID> fetchCandidates(UUID userId, Collection<UUID> excludeUserIds);

    List<ProfileResponse> batchProfiles(List<UUID> ids);
}