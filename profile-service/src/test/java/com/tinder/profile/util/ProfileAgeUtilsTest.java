package com.tinder.profile.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ProfileAgeUtils")
class ProfileAgeUtilsTest {

    @Test
    @DisplayName("calculates age from birth date")
    void calculateAge_ReturnsYears() {
        LocalDate birthDate = LocalDate.now().minusYears(25).minusDays(1);

        assertEquals(25, ProfileAgeUtils.calculateAge(birthDate));
    }

    @Test
    @DisplayName("returns zero when birth date is null")
    void nullBirthDate_ReturnsZero() {
        assertEquals(0, ProfileAgeUtils.calculateAge(null));
    }
}
