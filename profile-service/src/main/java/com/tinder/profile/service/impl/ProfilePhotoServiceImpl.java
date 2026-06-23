package com.tinder.profile.service.impl;

import com.tinder.profile.domain.Profile;
import com.tinder.profile.event.ProfileChangedEvent;
import com.tinder.profile.exception.EmptyOrNullValueException;
import com.tinder.profile.exception.ProfileNotFoundException;
import com.tinder.profile.mapper.ProfileMapper;
import com.tinder.profile.properties.ProfileProperties;
import com.tinder.profile.repository.ProfileRepository;
import com.tinder.profile.service.interfaces.ProfilePhotoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilePhotoServiceImpl implements ProfilePhotoService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final ProfileProperties profileProperties;
    private final ApplicationEventPublisher eventPublisher;

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
    public void reorderPhotos(UUID userId, List<String> photoUrls) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            throw new EmptyOrNullValueException("Photos list can't be empty");
        }

        Profile profile = requireProfile(userId);
        List<String> currentPhotos = profile.getPhotos();

        if (photoUrls.size() != currentPhotos.size() || !new java.util.HashSet<>(currentPhotos).containsAll(photoUrls)) {
            throw new IllegalArgumentException("The provided photos list does not match the existing photos exactly.");
        }

        profile.setPhotos(photoUrls);
        profile = profileRepository.save(profile);
        eventPublisher.publishEvent(new ProfileChangedEvent(profileMapper.toDto(profile)));
    }

    private Profile requireProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile with id " + userId + " not found"));
    }
}
