package com.tinder.profile.dto;

import com.tinder.profile.domain.Gender;

public record UserPreferencesResponse(
        Gender gender,
        Gender targetGender,
        Integer minAge,
        Integer maxAge,
        Double maxDistanceKm
) {
}