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

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    @Override
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
    public ProfileResponse getMyProfile() {
        UUID userId = getUserIdFromAuthentication();

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Профіль не знайдено. Будь ласка, пройдіть онбординг."));

        return profileMapper.toDto(profile);
    }

    private UUID getUserIdFromAuthentication() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
