package com.tinder.profile.dto;

import java.time.LocalDate;
import java.util.List;

public record UpdateProfileRequest(
        String name,
        LocalDate birthDate,
        String bio,
        List<String> interests
) {
}