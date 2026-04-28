package com.tinder.profile.service.impl;

import com.tinder.profile.domain.Profile;
import com.tinder.profile.domain.UserPreferences;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.LocationUpdateRequest;
import com.tinder.profile.dto.ProfileCandidateDto;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.dto.UpdatePreferencesRequest;
import com.tinder.profile.dto.UpdateProfileRequest;
import com.tinder.profile.dto.UserPreferencesResponse;
import com.tinder.profile.event.ActivityType;
import com.tinder.profile.event.ProfileChangedEvent;
import com.tinder.profile.exception.EmptyOrNullValueException;
import com.tinder.profile.exception.ProfileNotFoundException;
import com.tinder.profile.mapper.ProfileMapper;
import com.tinder.profile.producer.UserActivityProducer;
import com.tinder.profile.repository.ProfileRepository;
import com.tinder.profile.repository.ProfileSearchRepository;
import com.tinder.profile.service.interfaces.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileSearchRepository profileSearchRepository;
    private final ProfileMapper profileMapper;
    private final UserActivityProducer activityProducer;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ProfileResponse createProfile(UUID userId, CreateProfileRequest request) {
        if (profileRepository.existsByUserId(userId)) {
            throw new IllegalStateException("There is already a profile with userId " + userId);
        }
        Profile profile = profileMapper.toModel(request);
        profile.setUserId(userId);

        profile = profileRepository.save(profile);
        ProfileResponse response = profileMapper.toDto(profile);

        eventPublisher.publishEvent(new ProfileChangedEvent(response));

        return response;
    }

    @Override
    public ProfileResponse getMyProfile(UUID userId) {

        Profile profile = getProfileEntity(userId);

        return profileMapper.toDto(profile);
    }

    @Override
    public void deleteProfile(UUID userId) {
        Profile profile = getProfileEntity(userId);
        profileRepository.delete(profile);
    }

    @Override
    public UserPreferencesResponse getMyPreferences(UUID userId) {
        Profile profile = getProfileEntity(userId);

        return profileMapper.toUserPreferencesResponse(profile);
    }

    @Override
    public UserPreferencesResponse updateMyPreferences(UUID userId, UpdatePreferencesRequest request) {
        Profile profile = getProfileEntity(userId);

        profile = profileRepository.save(profileMapper.updatePreferencesFromDto(request, profile));

        eventPublisher.publishEvent(new ProfileChangedEvent(profileMapper.toDto(profile)));

        return profileMapper.toUserPreferencesResponse(profile);
    }

    @Override
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        Profile profile = getProfileEntity(userId);

        profile = profileRepository.save(profileMapper.updateEntityFromDto(request, profile));

        ProfileResponse response = profileMapper.toDto(profile);

        eventPublisher.publishEvent(new ProfileChangedEvent(response));

        return response;
    }


    @Override
    public void addPhotosToProfile(UUID userId, List<String> photoUrls) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            throw new EmptyOrNullValueException("Photos can't be empty or null");
        }

        Profile profile = getProfileEntity(userId);
        profile.getPhotos().addAll(photoUrls);
        profile = profileRepository.save(profile);
        eventPublisher.publishEvent(new ProfileChangedEvent(profileMapper.toDto(profile)));
    }

    @Override
    public void updateLocation(UUID userId, LocationUpdateRequest request) {

        GeoJsonPoint point = new GeoJsonPoint(request.longitude(), request.latitude());

        Profile profile = getProfileEntity(userId);
        profile.setLocation(point);
        profile.setLastSeen(LocalDateTime.now());
        profile = profileRepository.save(profile);
        eventPublisher.publishEvent(new ProfileChangedEvent(profileMapper.toDto(profile)));
        activityProducer.publishActivity(userId, ActivityType.LOCATION_UPDATE);
    }

    @Override
    public List<UUID> getCandidatesForFeed(UUID userId, int limit) {
        Profile searcher = getProfileEntity(userId);

        if (searcher.getLocation() == null) {
            throw new IllegalStateException("User location is not set. Cannot generate feed.");
        }

        UserPreferences prefs = searcher.getPreferences();
        if (prefs == null) {
            throw new EmptyOrNullValueException("User preferences are missing for user: " + userId);
        }

        LocalDate now = LocalDate.now();
        LocalDate maxBirthDate = now.minusYears(prefs.getMinAge());
        LocalDate minBirthDate = now.minusYears(prefs.getMaxAge() + 1).plusDays(1);

        double currentRadius = prefs.getMaxDistanceKm();

        List<ProfileCandidateDto> candidates = profileSearchRepository.findCandidates(
                prefs.getTargetGender(), minBirthDate, maxBirthDate,
                searcher.getLocation(), currentRadius, searcher.getInterests(), limit
        );

        if (candidates.size() < 50) {
            log.info("Not enough candidates for user {}. Relaxing search constraints.", userId);

            double relaxedRadius = currentRadius * 3.0;
            LocalDate relaxedMinBirth = minBirthDate.minusYears(2);
            LocalDate relaxedMaxBirth = maxBirthDate.plusYears(2);

            candidates = profileSearchRepository.findCandidates(
                    prefs.getTargetGender(), relaxedMinBirth, relaxedMaxBirth,
                    searcher.getLocation(), relaxedRadius, searcher.getInterests(), limit
            );
        }

        return candidates.stream()
                .map(ProfileCandidateDto::userId)
                .filter(id -> !id.equals(userId))
                .toList();
    }

    @Override
    public List<ProfileResponse> getBatchProfiles(List<UUID> ids) {
        List<Profile> profiles = profileRepository.findAllByUserIdIn(ids);

        return profiles.stream().map(profileMapper::toDto).toList();
    }

    public Profile getProfileEntity(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile with id " + userId + " not found"));
    }
}
