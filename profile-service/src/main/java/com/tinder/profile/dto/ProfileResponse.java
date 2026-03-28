package com.tinder.profile.dto;

import com.tinder.profile.domain.Gender;

import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String name,
        int age,
        Gender gender,
        String bio,
        List<String> interests,
        List<String> photos
) {
}