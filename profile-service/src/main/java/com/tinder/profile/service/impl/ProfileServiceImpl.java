package com.tinder.profile.service.impl;

import com.tinder.profile.domain.Profile;
import com.tinder.profile.domain.UserPreferences;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.LocationUpdateRequest;
import com.tinder.profile.dto.ProfileCandidateDto;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.exception.EmptyOrNullValueException;
import com.tinder.profile.exception.ProfileNotFoundException;
import com.tinder.profile.mapper.ProfileMapper;
import com.tinder.profile.repository.ProfileRepository;
import com.tinder.profile.repository.ProfileSearchRepository;
import com.tinder.profile.service.interfaces.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileSearchRepository profileSearchRepository;
    private final ProfileMapper profileMapper;

    @Override
    @Transactional
    public ProfileResponse createProfile(String userId, CreateProfileRequest request) {
        UUID userIdUUID = UUID.fromString(userId);

        if (profileRepository.existsByUserId(userIdUUID)) {
            throw new IllegalStateException("There is already a profile with userId " + userId);
        }
        Profile profile = profileMapper.toModel(request);
        profile.setUserId(userIdUUID);
        profile.setPhotos(new ArrayList<>());

        return profileMapper.toDto(profileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(String userId) {
        UUID userIdUUID = UUID.fromString(userId);

        Profile profile = getProfile(userIdUUID);

        return profileMapper.toDto(profile);
    }

    @Override
    @Transactional
    public void addPhotosToProfile(UUID userId, List<String> photoUrls) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            throw new EmptyOrNullValueException("Photos can't be empty or null");
        }

        Profile profile = getProfile(userId);
        profile.getPhotos().addAll(photoUrls);
        profileRepository.save(profile);
    }

    @Override
    @Transactional
    public void updateLocation(String userId, LocationUpdateRequest request) {
        UUID userIdUUID = UUID.fromString(userId);

        GeoJsonPoint point = new GeoJsonPoint(request.longitude(), request.latitude());

        Profile profile = getProfile(userIdUUID);
        profile.setLocation(point);
        profile.setLastSeen(LocalDateTime.now());
        profileRepository.save(profile);
    }

    @Override
    public List<UUID> getCandidatesForFeed(UUID userId) {
        Profile searcher = getProfile(userId);

        UserPreferences prefs = searcher.getPreferences();
        if (prefs == null) {
            throw new EmptyOrNullValueException("User preferences are missing for user: " + userId);
        }

        LocalDate now = LocalDate.now();
        LocalDate maxBirthDate = now.minusYears(prefs.getMinAge());
        LocalDate minBirthDate = now.minusYears(prefs.getMaxAge() + 1).plusDays(1);

        int targetLimit = 500;
        double currentRadius = prefs.getMaxDistanceKm();

        List<ProfileCandidateDto> candidates = profileSearchRepository.findCandidates(
                prefs.getTargetGender(), minBirthDate, maxBirthDate,
                searcher.getLocation(), currentRadius, searcher.getInterests(), targetLimit
        );

        if (candidates.size() < 50) {
            log.info("Not enough candidates for user {}. Relaxing search constraints.", userId);

            double relaxedRadius = currentRadius * 3.0;
            LocalDate relaxedMinBirth = minBirthDate.minusYears(2);
            LocalDate relaxedMaxBirth = maxBirthDate.plusYears(2);

            candidates = profileSearchRepository.findCandidates(
                    prefs.getTargetGender(), relaxedMinBirth, relaxedMaxBirth,
                    searcher.getLocation(), relaxedRadius, searcher.getInterests(), targetLimit
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

    private Profile getProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile with id " + userId + " not found"));
    }
}
