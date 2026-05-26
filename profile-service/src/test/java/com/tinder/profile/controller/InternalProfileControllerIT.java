package com.tinder.profile.controller;

import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.repository.ProfileRepository;
import com.tinder.profile.service.interfaces.ProfileService;
import com.tinder.profile.util.BaseIT;
import com.tinder.profile.util.ProfileTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Internal Profile Controller — Integration Tests")
class InternalProfileControllerIT extends BaseIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ProfileRepository profileRepository;

    private UUID searcherId;
    private UUID candidateId;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
        searcherId = UUID.randomUUID();
        candidateId = UUID.randomUUID();

        profileService.createProfile(searcherId, ProfileTestFixtures.validCreateRequest());
        profileService.updateLocation(searcherId, ProfileTestFixtures.locationRequest());
        ProfileTestFixtures.seedProfile(profileRepository, candidateId, true);
    }

    @Nested
    @DisplayName("GET /api/v1/internal/profiles/candidates")
    class GetCandidates {

        @Test
        @DisplayName("returns candidate ids without gateway headers")
        void internalCall_ReturnsCandidates() {
            ResponseEntity<List<UUID>> response = restTemplate.exchange(
                    "/api/v1/internal/profiles/candidates?userId={userId}&limit=10",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    },
                    searcherId
            );

            assertAll(
                    () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
                    () -> assertTrue(response.getBody() != null),
                    () -> assertFalse(response.getBody().contains(searcherId))
            );
        }

        @Test
        @DisplayName("excludes provided user ids")
        void excludeParam_ExcludesUsers() {
            ResponseEntity<List<UUID>> response = restTemplate.exchange(
                    "/api/v1/internal/profiles/candidates?userId={userId}&limit=10&excludeUserIds={exclude}",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    },
                    searcherId,
                    candidateId
            );

            assertTrue(response.getBody() == null || !response.getBody().contains(candidateId));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/internal/profiles/batch")
    class BatchProfiles {

        @Test
        @DisplayName("returns profiles for requested ids")
        void validIds_ReturnsProfiles() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<UUID>> entity = new HttpEntity<>(List.of(searcherId, candidateId), headers);

            ResponseEntity<List<ProfileResponse>> response = restTemplate.exchange(
                    "/api/v1/internal/profiles/batch",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertAll(
                    () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
                    () -> assertEquals(2, response.getBody().size())
            );
        }
    }
}
