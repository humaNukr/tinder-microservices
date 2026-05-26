package com.tinder.profile.util;

import java.time.LocalDate;
import java.time.Period;

public final class ProfileAgeUtils {

    private ProfileAgeUtils() {
    }

    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
