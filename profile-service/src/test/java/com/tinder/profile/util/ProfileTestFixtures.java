package com.tinder.profile.util;

import com.tinder.profile.domain.Gender;
import com.tinder.profile.domain.Profile;
import com.tinder.profile.domain.UserPreferences;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.LocationUpdateRequest;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.dto.UpdatePreferencesRequest;
import com.tinder.profile.dto.UpdateProfileRequest;
import com.tinder.profile.repository.ProfileRepository;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ProfileTestFixtures {

    private ProfileTestFixtures() {
    }

    public static CreateProfileRequest validCreateRequest() {
        return new CreateProfileRequest(
                "Alex",
                LocalDate.now().minusYears(25),
                Gender.MALE.name(),
                Gender.FEMALE.name(),
                "Bio text",
                List.of("music", "travel")
        );
    }

    public static UpdateProfileRequest updateProfileRequest() {
        return new UpdateProfileRequest(
                "Alex Updated",
                LocalDate.now().minusYears(26),
                "New bio",
                List.of("sports")
        );
    }

    public static UpdatePreferencesRequest updatePreferencesRequest() {
        return new UpdatePreferencesRequest(
                null,
                Gender.FEMALE,
                21,
                35,
                40.0
        );
    }

    public static LocationUpdateRequest locationRequest() {
        return new LocationUpdateRequest(30.5234, 50.4501);
    }

    public static ProfileResponse sampleResponse(UUID userId) {
        return new ProfileResponse(
                userId,
                "Alex",
                25,
                Gender.MALE,
                "Bio",
                List.of("music"),
                List.of()
        );
    }

    public static Profile seedProfile(ProfileRepository repository, UUID userId) {
        return seedProfile(repository, userId, false);
    }

    public static Profile seedProfile(ProfileRepository repository, UUID userId, boolean withLocation) {
        return seedProfile(repository, userId, withLocation, Gender.FEMALE);
    }

    public static Profile seedProfile(
            ProfileRepository repository,
            UUID userId,
            boolean withLocation,
            Gender gender
    ) {
        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setName("Alex");
        profile.setBirthDate(LocalDate.now().minusYears(25));
        profile.setGender(gender);
        profile.setBio("Bio");
        profile.setInterests(List.of("music"));

        UserPreferences prefs = new UserPreferences();
        prefs.setTargetGender(Gender.FEMALE);
        prefs.setMinAge(18);
        prefs.setMaxAge(35);
        prefs.setMaxDistanceKm(50.0);
        profile.setPreferences(prefs);

        if (withLocation) {
            profile.setLocation(new GeoJsonPoint(30.52, 50.45));
        }

        return repository.save(profile);
    }
}
