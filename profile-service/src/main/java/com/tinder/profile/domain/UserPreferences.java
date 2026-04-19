package com.tinder.profile.domain;

import lombok.Data;

@Data
public class UserPreferences {
    private Gender targetGender;
    private Integer minAge;
    private Integer maxAge;
    private Double maxDistanceKm;
}