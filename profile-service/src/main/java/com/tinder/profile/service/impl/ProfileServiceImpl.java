package com.tinder.profile.service.impl;

import com.tinder.profile.domain.Profile;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.LocationUpdateRequest;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.exception.EmptyOrNullValueException;
import com.tinder.profile.exception.ProfileNotFoundException;
import com.tinder.profile.mapper.ProfileMapper;
import com.tinder.profile.repository.ProfileRepository;
import com.tinder.profile.service.interfaces.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
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

    private Profile getProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile with id " + userId + " not found"));
    }
}
