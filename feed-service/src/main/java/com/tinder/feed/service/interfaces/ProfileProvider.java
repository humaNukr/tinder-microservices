package com.tinder.feed.service.interfaces;

import com.tinder.feed.dto.ProfileResponse;

import java.util.List;
import java.util.UUID;

public interface ProfileProvider {

    List<UUID> fetchCandidates(UUID userId);

    List<ProfileResponse> batchProfiles(List<UUID> ids);
}