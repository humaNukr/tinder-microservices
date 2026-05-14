package com.tinder.chat.application.port.out.profile;

import com.tinder.chat.shared.dto.external.ProfileResponse;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ProfilePort {
    Map<UUID, ProfileResponse> getProfilesMap(Set<UUID> userIds);
}