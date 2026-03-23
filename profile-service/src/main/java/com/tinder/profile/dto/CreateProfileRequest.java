package com.tinder.profile.dto;

import com.tinder.profile.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateProfileRequest(
        @NotBlank @Size(min = 2, max = 50) String name,
        @NotNull @Past LocalDate birthDate,
        @NotNull Gender gender,
        @NotNull Gender targetGender,
        @Size(max = 500) String bio,
        List<String> interests
) {
}