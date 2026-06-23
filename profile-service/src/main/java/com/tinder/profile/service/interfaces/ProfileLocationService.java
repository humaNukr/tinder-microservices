package com.tinder.profile.service.interfaces;

import com.tinder.profile.dto.LocationUpdateRequest;

import java.time.Instant;
import java.util.UUID;

public interface ProfileLocationService {
    void updateLocation(UUID userId, LocationUpdateRequest request);

    void updateLastSeen(UUID userId, Instant timestamp);
}
