package com.tinder.chat.infrastructure.adapter;

import com.tinder.chat.chat.dto.ProfileResponse;

import java.util.List;
import java.util.UUID;

public interface ProfileServiceClient {
    List<ProfileResponse> batchProfiles(List<UUID> ids);
}
