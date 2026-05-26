package com.tinder.profile.service;

import com.mongodb.client.result.UpdateResult;
import com.tinder.profile.domain.Profile;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.ProfileCandidateDto;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.event.ProfileChangedEvent;
import com.tinder.profile.exception.EmptyOrNullValueException;
import com.tinder.profile.exception.ProfileNotFoundException;
import com.tinder.profile.mapper.ProfileMapper;
import com.tinder.profile.producer.UserActivityProducer;
import com.tinder.profile.properties.ProfileProperties;
import com.tinder.profile.repository.ProfileRepository;
import com.tinder.profile.repository.ProfileSearchRepository;
import com.tinder.profile.service.impl.ProfileServiceImpl;
import com.tinder.profile.service.interfaces.ProfileCacheService;
import com.tinder.profile.storage.StorageService;
import com.tinder.profile.util.ProfileTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileServiceImpl")
class ProfileServiceImplTest {

    private final UUID userId = UUID.randomUUID();
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private ProfileSearchRepository profileSearchRepository;
    @Mock
    private ProfileMapper profileMapper;
    @Mock
    private UserActivityProducer activityProducer;
    @Mock
    private ProfileProperties profileProperties;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private ProfileCacheService profileCacheService;
    @Mock
    private StorageService storageService;
    @InjectMocks
    private ProfileServiceImpl profileService;

    private Profile buildSearcher() {
        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setLocation(new GeoJsonPoint(30.52, 50.45));
        com.tinder.profile.domain.UserPreferences prefs = new com.tinder.profile.domain.UserPreferences();
        prefs.setTargetGender(com.tinder.profile.domain.Gender.FEMALE);
        prefs.setMinAge(18);
        prefs.setMaxAge(35);
        prefs.setMaxDistanceKm(50.0);
        profile.setPreferences(prefs);
        profile.setInterests(List.of("music"));
        return profile;
    }

    @Nested
    @DisplayName("createProfile()")
    class CreateProfile {

        @Test
        @DisplayName("throws when profile already exists")
        void duplicateUser_ThrowsIllegalState() {
            when(profileRepository.existsByUserId(userId)).thenReturn(true);

            assertThrows(IllegalStateException.class,
                    () -> profileService.createProfile(userId, ProfileTestFixtures.validCreateRequest()));
        }

        @Test
        @DisplayName("throws when user is under minimum age")
        void underMinAge_ThrowsIllegalArgument() {
            when(profileRepository.existsByUserId(userId)).thenReturn(false);
            when(profileProperties.minAge()).thenReturn(18);

            CreateProfileRequest request = new CreateProfileRequest(
                    "Kid",
                    LocalDate.now().minusYears(16),
                    "MALE",
                    "FEMALE",
                    null,
                    List.of()
            );

            assertThrows(IllegalArgumentException.class, () -> profileService.createProfile(userId, request));
        }
    }

    @Nested
    @DisplayName("getMyProfile()")
    class GetMyProfile {

        @Test
        @DisplayName("returns cached profile when present")
        void cacheHit_ReturnsCached() {
            ProfileResponse cached = ProfileTestFixtures.sampleResponse(userId);
            when(profileCacheService.getCachedProfile(userId)).thenReturn(Optional.of(cached));

            ProfileResponse result = profileService.getMyProfile(userId);

            assertEquals(cached, result);
            verify(profileRepository, never()).findByUserId(any());
        }

        @Test
        @DisplayName("loads from DB and caches on miss")
        void cacheMiss_LoadsAndCaches() {
            Profile profile = new Profile();
            profile.setUserId(userId);
            ProfileResponse dto = ProfileTestFixtures.sampleResponse(userId);

            when(profileCacheService.getCachedProfile(userId)).thenReturn(Optional.empty());
            when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
            when(profileMapper.toDto(profile)).thenReturn(dto);

            ProfileResponse result = profileService.getMyProfile(userId);

            assertEquals(dto, result);
            verify(profileCacheService).cacheProfile(dto);
        }
    }

    @Nested
    @DisplayName("deleteAccountData()")
    class DeleteAccountData {

        @Test
        @DisplayName("deletes photos, profile and evicts cache")
        void existingProfile_DeletesAll() {
            Profile profile = new Profile();
            profile.setUserId(userId);
            profile.setPhotos(List.of("tinder-media/u1/photo.jpg"));

            when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

            profileService.deleteAccountData(userId);

            verify(storageService).deleteFiles(profile.getPhotos());
            verify(profileRepository).delete(profile);
            verify(profileCacheService).evictProfile(userId);
        }

        @Test
        @DisplayName("does nothing when profile is absent")
        void missingProfile_NoOp() {
            when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

            profileService.deleteAccountData(userId);

            verify(storageService, never()).deleteFiles(any());
            verify(profileRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("addPhotosToProfile()")
    class AddPhotos {

        @Test
        @DisplayName("throws when photo list is empty")
        void emptyList_Throws() {
            assertThrows(EmptyOrNullValueException.class,
                    () -> profileService.addPhotosToProfile(userId, List.of()));
        }

        @Test
        @DisplayName("throws when max photos exceeded")
        void exceedsMax_Throws() {
            Profile profile = new Profile();
            profile.setUserId(userId);
            profile.setPhotos(new java.util.ArrayList<>(List.of("a", "b")));

            when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
            when(profileProperties.maxPhotos()).thenReturn(2);

            assertThrows(IllegalArgumentException.class,
                    () -> profileService.addPhotosToProfile(userId, List.of("c")));
        }
    }

    @Nested
    @DisplayName("getCandidatesForFeed()")
    class GetCandidates {

        @Test
        @DisplayName("throws when location is not set")
        void noLocation_Throws() {
            Profile profile = new Profile();
            profile.setUserId(userId);
            profile.setPreferences(new com.tinder.profile.domain.UserPreferences());

            when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

            assertThrows(IllegalStateException.class,
                    () -> profileService.getCandidatesForFeed(userId, 10, Set.of()));
        }

        @Test
        @DisplayName("relaxes search when not enough candidates")
        void belowLimit_RelaxesSearch() {
            Profile profile = buildSearcher();
            UUID candidateId = UUID.randomUUID();

            when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
            when(profileSearchRepository.findCandidates(any(), any(), any(), any(), any(), any(), eq(10), any()))
                    .thenReturn(List.of())
                    .thenReturn(List.of(new ProfileCandidateDto(candidateId)));

            List<UUID> result = profileService.getCandidatesForFeed(userId, 10, Set.of());

            assertEquals(List.of(candidateId), result);
            verify(profileSearchRepository, org.mockito.Mockito.times(2))
                    .findCandidates(any(), any(), any(), any(), any(), any(), eq(10), any());
        }
    }

    @Nested
    @DisplayName("updateLocation()")
    class UpdateLocation {

        @Test
        @DisplayName("throws when profile not found")
        void notFound_Throws() {
            UpdateResult updateResult = mock(UpdateResult.class);
            when(updateResult.getMatchedCount()).thenReturn(0L);
            when(mongoTemplate.updateFirst(any(), any(), eq(Profile.class))).thenReturn(updateResult);

            assertThrows(ProfileNotFoundException.class,
                    () -> profileService.updateLocation(userId, ProfileTestFixtures.locationRequest()));
        }

        @Test
        @DisplayName("publishes profile changed event and activity on success")
        void success_PublishesEvents() {
            Profile profile = buildSearcher();
            ProfileResponse dto = ProfileTestFixtures.sampleResponse(userId);
            UpdateResult updateResult = mock(UpdateResult.class);

            when(updateResult.getMatchedCount()).thenReturn(1L);
            when(mongoTemplate.updateFirst(any(), any(), eq(Profile.class))).thenReturn(updateResult);
            when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
            when(profileMapper.toDto(profile)).thenReturn(dto);

            profileService.updateLocation(userId, ProfileTestFixtures.locationRequest());

            ArgumentCaptor<ProfileChangedEvent> eventCaptor = ArgumentCaptor.forClass(ProfileChangedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertEquals(dto, eventCaptor.getValue().response());
            verify(activityProducer).publishActivity(userId, com.tinder.profile.event.ActivityType.LOCATION_UPDATE);
        }
    }

    @Nested
    @DisplayName("updateMyPreferences()")
    class UpdatePreferences {

        @Test
        @DisplayName("throws when profile not found")
        void notFound_Throws() {
            when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThrows(ProfileNotFoundException.class,
                    () -> profileService.updateMyPreferences(userId, ProfileTestFixtures.updatePreferencesRequest()));
        }
    }
}
