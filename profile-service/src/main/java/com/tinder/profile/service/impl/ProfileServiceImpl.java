package com.tinder.profile.service.impl;

import com.tinder.profile.domain.Profile;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.exception.ProfileNotFoundException;
import com.tinder.profile.mapper.ProfileMapper;
import com.tinder.profile.repository.ProfileRepository;
import com.tinder.profile.service.interfaces.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public ProfileResponse createProfile(CreateProfileRequest request) {
        UUID userId = getUserIdFromAuthentication();

        if (profileRepository.existsByUserId(userId)) {
            throw new IllegalStateException("There is already a profile with userId " + userId);
        }
        Profile profile = profileMapper.toModel(request);
        profile.setUserId(userId);
        profile.setPhotos(new ArrayList<>());

        return profileMapper.toDto(profileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        UUID userId = getUserIdFromAuthentication();

        Profile profile = getProfile(userId);

        return profileMapper.toDto(profile);
    }

    @Override
    @Transactional
    public void addPhotosToProfile(UUID userId, List<String> photoUrls) {
        UUID profileId = getUserIdFromAuthentication();

        if (!profileId.equals(userId)) {
            throw new IllegalArgumentException("User id is not the same as the requested profile id");
        }

        Profile profile = getProfile(userId);
        profile.getPhotos().addAll(photoUrls);
        profileRepository.save(profile);
    }

    private UUID getUserIdFromAuthentication() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private Profile getProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile with id " + userId + " not found"));
    }
}
