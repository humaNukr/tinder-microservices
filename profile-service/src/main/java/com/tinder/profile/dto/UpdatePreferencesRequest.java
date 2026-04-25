package com.tinder.profile.dto;

import com.tinder.profile.domain.Gender;
import com.tinder.profile.validation.ValueOfEnum;
import jakarta.validation.constraints.Min;

public record UpdatePreferencesRequest(
        @ValueOfEnum(enumClass = Gender.class, message = "Invalid gender") Gender gender,
        Gender targetGender,
        @Min(message = "Applicants under 18 are not eligible", value = 18) Integer minAge,
        Integer maxAge,
        Double maxDistanceKm
) {
}