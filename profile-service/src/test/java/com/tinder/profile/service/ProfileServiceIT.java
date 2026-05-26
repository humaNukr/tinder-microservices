package com.tinder.profile.service;

import com.tinder.profile.domain.Profile;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.dto.UpdateProfileRequest;
import com.tinder.profile.exception.ProfileNotFoundException;
import com.tinder.profile.repository.ProfileRepository;
import com.tinder.profile.service.interfaces.ProfileCacheService;
import com.tinder.profile.service.interfaces.ProfileService;
import com.tinder.profile.util.BaseIT;
import com.tinder.profile.util.ProfileTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProfileService — Integration Tests")
class ProfileServiceIT extends BaseIT {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ProfileCacheService profileCacheService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profileRepository.deleteAll();
        profileCacheService.evictProfile(userId);
    }

    @Nested
    @DisplayName("createProfile()")
    class CreateProfile {

        @Test
        @DisplayName("persists profile with default preferences")
        void validRequest_SavesProfile() {
            ProfileResponse response = profileService.createProfile(userId, ProfileTestFixtures.validCreateRequest());

            Profile saved = profileRepository.findByUserId(userId).orElseThrow();

            assertAll(
                    () -> assertEquals(userId, response.userId()),
                    () -> assertEquals("Alex", saved.getName()),
                    () -> assertEquals(18, saved.getPreferences().getMinAge()),
                    () -> assertEquals(99, saved.getPreferences().getMaxAge()),
                    () -> assertEquals(50.0, saved.getPreferences().getMaxDistanceKm())
            );
        }

        @Test
        @DisplayName("throws when profile already exists")
        void duplicate_Throws() {
            profileService.createProfile(userId, ProfileTestFixtures.validCreateRequest());

            assertThrows(IllegalStateException.class,
                    () -> profileService.createProfile(userId, ProfileTestFixtures.validCreateRequest()));
        }
    }

    @Nested
    @DisplayName("getMyProfile()")
    class GetMyProfile {

        @Test
        @DisplayName("returns profile from database")
        void existingProfile_ReturnsDto() {
            ProfileTestFixtures.seedProfile(profileRepository, userId);

            ProfileResponse response = profileService.getMyProfile(userId);

            assertEquals("Alex", response.name());
        }

        @Test
        @DisplayName("uses cache on second read")
        void secondRead_UsesCache() {
            profileService.createProfile(userId, ProfileTestFixtures.validCreateRequest());

            profileService.getMyProfile(userId);
            profileRepository.deleteAll();

            ProfileResponse cached = profileService.getMyProfile(userId);

            assertEquals(userId, cached.userId());
            assertFalse(profileRepository.existsByUserId(userId));
        }
    }

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("updates name and bio")
        void validUpdate_PersistsChanges() {
            profileService.createProfile(userId, ProfileTestFixtures.validCreateRequest());
            UpdateProfileRequest update = ProfileTestFixtures.updateProfileRequest();

            ProfileResponse response = profileService.updateProfile(userId, update);

            assertAll(
                    () -> assertEquals("Alex Updated", response.name()),
                    () -> assertEquals("New bio", profileRepository.findByUserId(userId).orElseThrow().getBio())
            );
        }
    }

    @Nested
    @DisplayName("updateLocation()")
    class UpdateLocation {

        @Test
        @DisplayName("sets geo location on profile")
        void validCoords_SetsLocation() {
            profileService.createProfile(userId, ProfileTestFixtures.validCreateRequest());

            profileService.updateLocation(userId, ProfileTestFixtures.locationRequest());

            Profile saved = profileRepository.findByUserId(userId).orElseThrow();
            assertAll(
                    () -> assertTrue(saved.getLocation() != null),
                    () -> assertEquals(30.5234, saved.getLocation().getX()),
                    () -> assertEquals(50.4501, saved.getLocation().getY())
            );
        }
    }

    @Nested
    @DisplayName("deleteProfile()")
    class DeleteProfile {

        @Test
        @DisplayName("removes profile and evicts cache")
        void existingProfile_Deleted() {
            profileService.createProfile(userId, ProfileTestFixtures.validCreateRequest());
            profileService.getMyProfile(userId);

            profileService.deleteProfile(userId);

            assertAll(
                    () -> assertFalse(profileRepository.existsByUserId(userId)),
                    () -> assertTrue(profileCacheService.getCachedProfile(userId).isEmpty())
            );
        }
    }

    @Nested
    @DisplayName("getCandidatesForFeed()")
    class GetCandidates {

        @Test
        @DisplayName("throws when location missing")
        void noLocation_Throws() {
            profileService.createProfile(userId, ProfileTestFixtures.validCreateRequest());

            assertThrows(IllegalStateException.class,
                    () -> profileService.getCandidatesForFeed(userId, 10, Set.of()));
        }

        @Test
        @DisplayName("excludes searcher and provided ids")
        void withLocation_ExcludesSelf() {
            profileService.createProfile(userId, ProfileTestFixtures.validCreateRequest());
            profileService.updateLocation(userId, ProfileTestFixtures.locationRequest());

            UUID other = UUID.randomUUID();
            ProfileTestFixtures.seedProfile(profileRepository, other, true);

            List<UUID> candidates = profileService.getCandidatesForFeed(userId, 10, Set.of());

            assertFalse(candidates.contains(userId));
        }
    }

    @Nested
    @DisplayName("getBatchProfiles()")
    class GetBatch {

        @Test
        @DisplayName("returns profiles for given ids")
        void existingIds_ReturnsDtos() {
            ProfileTestFixtures.seedProfile(profileRepository, userId);
            UUID other = UUID.randomUUID();
            ProfileTestFixtures.seedProfile(profileRepository, other);

            List<ProfileResponse> batch = profileService.getBatchProfiles(List.of(userId, other));

            assertEquals(2, batch.size());
        }
    }

    @Nested
    @DisplayName("updateLastSeen()")
    class UpdateLastSeen {

        @Test
        @DisplayName("updates lastSeen timestamp")
        void validTimestamp_UpdatesField() {
            profileService.createProfile(userId, ProfileTestFixtures.validCreateRequest());

            profileService.updateLastSeen(userId, java.time.Instant.parse("2026-01-15T10:00:00Z"));

            Profile saved = profileRepository.findByUserId(userId).orElseThrow();
            assertTrue(saved.getLastSeen() != null);
        }
    }

    @Nested
    @DisplayName("requireProfile edge cases")
    class NotFound {

        @Test
        @DisplayName("throws ProfileNotFoundException for unknown user")
        void unknownUser_Throws() {
            assertThrows(ProfileNotFoundException.class, () -> profileService.getMyPreferences(userId));
        }
    }
}
