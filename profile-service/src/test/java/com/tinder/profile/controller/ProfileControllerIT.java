package com.tinder.profile.controller;

import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.dto.UpdatePreferencesRequest;
import com.tinder.profile.dto.UpdateProfileRequest;
import com.tinder.profile.dto.UserPreferencesResponse;
import com.tinder.profile.dto.error.ErrorResponseDto;
import com.tinder.profile.repository.ProfileRepository;
import com.tinder.profile.util.BaseIT;
import com.tinder.profile.util.ProfileTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Profile Controller — Integration Tests")
class ProfileControllerIT extends BaseIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProfileRepository profileRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profileRepository.deleteAll();
    }

    private <T> HttpEntity<T> withUser(T body, UUID id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-User-Id", id.toString());
        return new HttpEntity<>(body, headers);
    }

    @Nested
    @DisplayName("POST /api/v1/profiles/onboarding")
    class CreateProfile {

        @Test
        @DisplayName("returns 201 and creates profile")
        void validRequest_ReturnsCreated() {
            ResponseEntity<ProfileResponse> response = restTemplate.exchange(
                    "/api/v1/profiles/onboarding",
                    HttpMethod.POST,
                    withUser(ProfileTestFixtures.validCreateRequest(), userId),
                    ProfileResponse.class
            );

            assertAll(
                    () -> assertEquals(HttpStatus.CREATED, response.getStatusCode()),
                    () -> assertNotNull(response.getBody()),
                    () -> assertEquals(userId, response.getBody().userId()),
                    () -> assertEquals(true, profileRepository.existsByUserId(userId))
            );
        }

        @Test
        @DisplayName("returns 400 when X-User-Id is missing")
        void missingUserHeader_Returns400() {
            ResponseEntity<ErrorResponseDto> response = restTemplate.exchange(
                    "/api/v1/profiles/onboarding",
                    HttpMethod.POST,
                    new HttpEntity<>(ProfileTestFixtures.validCreateRequest()),
                    ErrorResponseDto.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/profiles/me")
    class GetProfile {

        @Test
        @DisplayName("returns 200 for existing profile")
        void existingProfile_Returns200() {
            restTemplate.exchange(
                    "/api/v1/profiles/onboarding",
                    HttpMethod.POST,
                    withUser(ProfileTestFixtures.validCreateRequest(), userId),
                    ProfileResponse.class
            );

            ResponseEntity<ProfileResponse> response = restTemplate.exchange(
                    "/api/v1/profiles/me",
                    HttpMethod.GET,
                    withUser(null, userId),
                    ProfileResponse.class
            );

            assertAll(
                    () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
                    () -> assertEquals("Alex", response.getBody().name())
            );
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/profiles/me")
    class UpdateProfile {

        @Test
        @DisplayName("returns 200 and updates profile")
        void validUpdate_Returns200() {
            restTemplate.exchange(
                    "/api/v1/profiles/onboarding",
                    HttpMethod.POST,
                    withUser(ProfileTestFixtures.validCreateRequest(), userId),
                    ProfileResponse.class
            );

            UpdateProfileRequest update = ProfileTestFixtures.updateProfileRequest();
            ResponseEntity<ProfileResponse> response = restTemplate.exchange(
                    "/api/v1/profiles/me",
                    HttpMethod.PATCH,
                    withUser(update, userId),
                    ProfileResponse.class
            );

            assertEquals("Alex Updated", response.getBody().name());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/profiles/me/location")
    class UpdateLocation {

        @Test
        @DisplayName("returns 200 and sets location")
        void validCoords_Returns200() {
            restTemplate.exchange(
                    "/api/v1/profiles/onboarding",
                    HttpMethod.POST,
                    withUser(ProfileTestFixtures.validCreateRequest(), userId),
                    ProfileResponse.class
            );

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/profiles/me/location",
                    HttpMethod.PATCH,
                    withUser(ProfileTestFixtures.locationRequest(), userId),
                    Void.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/profiles/me/preferences")
    class Preferences {

        @Test
        @DisplayName("returns preferences for existing profile")
        void existingProfile_ReturnsPreferences() {
            restTemplate.exchange(
                    "/api/v1/profiles/onboarding",
                    HttpMethod.POST,
                    withUser(ProfileTestFixtures.validCreateRequest(), userId),
                    ProfileResponse.class
            );

            ResponseEntity<UserPreferencesResponse> response = restTemplate.exchange(
                    "/api/v1/profiles/me/preferences",
                    HttpMethod.GET,
                    withUser(null, userId),
                    UserPreferencesResponse.class
            );

            assertAll(
                    () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
                    () -> assertEquals(18, response.getBody().minAge())
            );
        }

        @Test
        @DisplayName("updates preferences via PATCH")
        void patchPreferences_UpdatesValues() {
            restTemplate.exchange(
                    "/api/v1/profiles/onboarding",
                    HttpMethod.POST,
                    withUser(ProfileTestFixtures.validCreateRequest(), userId),
                    ProfileResponse.class
            );

            UpdatePreferencesRequest update = ProfileTestFixtures.updatePreferencesRequest();
            ResponseEntity<UserPreferencesResponse> response = restTemplate.exchange(
                    "/api/v1/profiles/me/preferences",
                    HttpMethod.PATCH,
                    withUser(update, userId),
                    UserPreferencesResponse.class
            );

            assertEquals(21, response.getBody().minAge());
        }
    }
}
