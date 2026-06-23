package com.tinder.profile.service.impl;

import com.tinder.profile.domain.Gender;
import com.tinder.profile.domain.Profile;
import com.tinder.profile.domain.UserPreferences;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.dto.UpdatePreferencesRequest;
import com.tinder.profile.dto.UpdateProfileRequest;
import com.tinder.profile.dto.UserPreferencesResponse;
import com.tinder.profile.event.ProfileChangedEvent;
import com.tinder.profile.exception.ProfileNotFoundException;
import com.tinder.profile.mapper.ProfileMapper;
import com.tinder.profile.properties.ProfileProperties;
import com.tinder.profile.repository.ProfileRepository;
import com.tinder.profile.service.interfaces.ProfileCacheService;
import com.tinder.profile.service.interfaces.ProfileCoreService;
import com.tinder.profile.storage.StorageService;
import com.tinder.profile.util.ProfileAgeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileCoreServiceImpl implements ProfileCoreService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final ProfileProperties profileProperties;
    private final ApplicationEventPublisher eventPublisher;
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
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            profileRepository.delete(profile);
            profileCacheService.evictProfile(userId);
        });
    }

    @Override
    public void deleteAccountData(UUID userId) {
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            List<String> photoKeys = profile.getPhotos();
            if (photoKeys != null && !photoKeys.isEmpty()) {
                storageService.deleteFiles(photoKeys);
            }
            profileRepository.delete(profile);
            profileCacheService.evictProfile(userId);
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
    public List<ProfileResponse> getBatchProfiles(List<UUID> ids) {
        List<Profile> profiles = profileRepository.findAllByUserIdIn(ids);
        return profiles.stream().map(profileMapper::toDto).toList();
    }

    private Profile requireProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile with id " + userId + " not found"));
    }
}
