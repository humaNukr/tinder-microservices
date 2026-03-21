package com.tinder.profile.dto;

import com.tinder.profile.domain.Gender;

import java.util.List;

public record ProfileResponse(
        String id,
        String name,
        int age,
        Gender gender,
        String bio,
        List<String> interests,
        List<String> photos
) {
}