package com.tinder.profile.service.impl;

import com.tinder.profile.domain.Profile;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.ProfileResponse;
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
        String userIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID userId = UUID.fromString(userIdStr);

        if (profileRepository.existsByUserId(userId)) {
            throw new IllegalStateException("There is already a profile with userId " + userIdStr);
        }
        Profile profile = profileMapper.toModel(request);
        profile.setUserId(userId);
        profile.setPhotos(new ArrayList<>());

        return profileMapper.toDto(profileRepository.save(profile));
    }
}
