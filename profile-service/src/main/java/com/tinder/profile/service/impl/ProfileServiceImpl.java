package com.tinder.profile.service.impl;

import com.tinder.profile.domain.Gender;
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
import com.tinder.profile.properties.ProfileProperties;
import com.tinder.profile.repository.ProfileRepository;
import com.tinder.profile.repository.ProfileSearchRepository;
import com.tinder.profile.service.interfaces.ProfileCacheService;
import com.tinder.profile.service.interfaces.ProfileService;
import com.tinder.profile.storage.StorageService;
import com.tinder.profile.util.ProfileAgeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileSearchRepository profileSearchRepository;
    private final ProfileMapper profileMapper;
    private final UserActivityProducer activityProducer;
    private final ProfileProperties profileProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final MongoTemplate mongoTemplate;
    private final ProfileCacheService profileCacheService;
    private final StorageService storageService;

    @Override
    public ProfileResponse createProfile(UUID userId, CreateProfileRequest request) {
        if (profileRepository.existsByUserId(userId)) {
            throw new IllegalStateException("There is already a profile with userId " + userId);
        }

        int age = ProfileAgeUtils.calculateAge(request.birthDate());
        if (age < profileProperties.minAge()) {
            throw new IllegalArgumentException("User must be at least " + profileProperties.minAge() + " years old");
        }

        Profile profile = profileMapper.toModel(request);
        profile.setUserId(userId);

        UserPreferences defaultPrefs = new UserPreferences();
        defaultPrefs.setMinAge(profileProperties.minAge());
        defaultPrefs.setMaxAge(99);
        defaultPrefs.setMaxDistanceKm(profileProperties.defaultSearchRadiusKm());

        if (request.targetGender() != null) {
            defaultPrefs.setTargetGender(Gender.valueOf(request.targetGender().toUpperCase()));
        }

        profile.setPreferences(defaultPrefs);

        profile = profileRepository.save(profile);
        ProfileResponse response = profileMapper.toDto(profile);

        eventPublisher.publishEvent(new ProfileChangedEvent(response));

        return response;
    }

    @Override
    public ProfileResponse getMyProfile(UUID userId) {
        return profileCacheService.getCachedProfile(userId)
                .orElseGet(() -> {
                    Profile profile = requireProfile(userId);
                    ProfileResponse response = profileMapper.toDto(profile);
                    profileCacheService.cacheProfile(response);
                    return response;
                });
    }

    @Override
    public void deleteProfile(UUID userId) {
        profileRepository.findByUserId(userId).ifPresent(profileRepository::delete);
    }

    @Override
    public void deleteAccountData(UUID userId) {
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            List<String> photoKeys = profile.getPhotos();
            if (photoKeys != null && !photoKeys.isEmpty()) {
                storageService.deleteFiles(photoKeys);
            }
            profileRepository.delete(profile);
            log.info("Deleted profile and photos for user {}", userId);
        });
    }

    @Override
    public UserPreferencesResponse getMyPreferences(UUID userId) {
        Profile profile = requireProfile(userId);

        return profileMapper.toUserPreferencesResponse(profile);
    }

    @Override
    public UserPreferencesResponse updateMyPreferences(UUID userId, UpdatePreferencesRequest request) {
        Profile profile = requireProfile(userId);

        profile = profileRepository.save(profileMapper.updatePreferencesFromDto(request, profile));

        eventPublisher.publishEvent(new ProfileChangedEvent(profileMapper.toDto(profile)));

        return profileMapper.toUserPreferencesResponse(profile);
    }

    @Override
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        Profile profile = requireProfile(userId);

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

        Profile profile = requireProfile(userId);

        if (profile.getPhotos().size() + photoUrls.size() > profileProperties.maxPhotos()) {
            throw new IllegalArgumentException(
                    "Profile cannot have more than " + profileProperties.maxPhotos() + " photos"
            );
        }

        profile.getPhotos().addAll(photoUrls);
        profile = profileRepository.save(profile);
        eventPublisher.publishEvent(new ProfileChangedEvent(profileMapper.toDto(profile)));
    }

    @Override
    public void updateLocation(UUID userId, LocationUpdateRequest request) {
        GeoJsonPoint point = new GeoJsonPoint(request.longitude(), request.latitude());
        LocalDateTime now = LocalDateTime.now();

        Query query = new Query(Criteria.where("userId").is(userId));
        Update update = new Update()
                .set("location", point)
                .set("lastSeen", now);

        var result = mongoTemplate.updateFirst(query, update, Profile.class);
        if (result.getMatchedCount() == 0) {
            throw new ProfileNotFoundException("Profile with id " + userId + " not found");
        }

        Profile profile = requireProfile(userId);
        eventPublisher.publishEvent(new ProfileChangedEvent(profileMapper.toDto(profile)));
        activityProducer.publishActivity(userId, ActivityType.LOCATION_UPDATE);
    }

    @Override
    public List<UUID> getCandidatesForFeed(UUID userId, int limit, Collection<UUID> excludeUserIds) {
        Profile searcher = requireProfile(userId);

        if (searcher.getLocation() == null) {
            throw new IllegalStateException("User location is not set. Cannot generate feed.");
        }

        UserPreferences prefs = searcher.getPreferences();
        if (prefs == null) {
            throw new EmptyOrNullValueException("User preferences are missing for user: " + userId);
        }

        Set<UUID> exclude = new HashSet<>();
        exclude.add(userId);
        if (excludeUserIds != null) {
            exclude.addAll(excludeUserIds);
        }

        LocalDate now = LocalDate.now();
        LocalDate maxBirthDate = now.minusYears(prefs.getMinAge());
        LocalDate minBirthDate = now.minusYears(prefs.getMaxAge() + 1).plusDays(1);

        double currentRadius = prefs.getMaxDistanceKm();

        List<ProfileCandidateDto> candidates = profileSearchRepository.findCandidates(
                prefs.getTargetGender(), minBirthDate, maxBirthDate,
                searcher.getLocation(), currentRadius, searcher.getInterests(), limit, exclude
        );

        if (candidates.size() < limit) {
            log.info("Not enough candidates for user {} ({} / {}). Relaxing search constraints.",
                    userId, candidates.size(), limit);

            double relaxedRadius = currentRadius * 3.0;
            LocalDate relaxedMinBirth = minBirthDate.minusYears(2);
            LocalDate relaxedMaxBirth = maxBirthDate.plusYears(2);

            candidates = profileSearchRepository.findCandidates(
                    prefs.getTargetGender(), relaxedMinBirth, relaxedMaxBirth,
                    searcher.getLocation(), relaxedRadius, searcher.getInterests(), limit, exclude
            );
        }

        return candidates.stream()
                .map(ProfileCandidateDto::userId)
                .toList();
    }

    @Override
    public List<ProfileResponse> getBatchProfiles(List<UUID> ids) {
        List<Profile> profiles = profileRepository.findAllByUserIdIn(ids);

        return profiles.stream().map(profileMapper::toDto).toList();
    }

    @Override
    public List<String> removePhotosFromProfile(UUID userId, List<String> photoUrlsToRemove) {
        if (photoUrlsToRemove == null || photoUrlsToRemove.isEmpty()) {
            return Collections.emptyList();
        }

        Profile profile = requireProfile(userId);
        List<String> currentPhotos = profile.getPhotos();

        List<String> validPhotosToRemove = photoUrlsToRemove.stream()
                .filter(currentPhotos::contains)
                .toList();

        if (validPhotosToRemove.isEmpty()) {
            return Collections.emptyList();
        }

        currentPhotos.removeAll(validPhotosToRemove);
        profile = profileRepository.save(profile);
        eventPublisher.publishEvent(new ProfileChangedEvent(profileMapper.toDto(profile)));

        return validPhotosToRemove;
    }

    @Override
    public void updateLastSeen(UUID userId, Instant timestamp) {
        LocalDateTime lastSeen = LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC);

        Query query = new Query(Criteria.where("userId").is(userId));
        Update update = new Update().max("lastSeen", lastSeen);

        mongoTemplate.updateFirst(query, update, Profile.class);

        log.debug("Attempted to update last_seen for user {} to {}", userId, lastSeen);
    }

    private Profile requireProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile with id " + userId + " not found"));
    }
}
