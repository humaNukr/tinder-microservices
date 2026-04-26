package com.tinder.profile.processor;

import com.tinder.profile.domain.InboxEvent;
import com.tinder.profile.domain.Profile;
import com.tinder.profile.event.UserActivityEvent;
import com.tinder.profile.exception.ProfileNotFoundException;
import com.tinder.profile.repository.InboxEventRepository;
import com.tinder.profile.service.interfaces.ProfilePhotoFacade;
import com.tinder.profile.service.interfaces.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityProcessor {

    private final InboxEventRepository inboxEventRepository;
    private final ProfileService profileService;
    private final ProfilePhotoFacade profilePhotoFacade;

    public void deleteProfileData(UserActivityEvent event) {
        UUID eventId = event.eventId();
        log.info("Processing DELETE_ACCOUNT event: {}", eventId);

        if (inboxEventRepository.existsById(eventId)) {
            log.warn("Duplicate DeleteAccount event detected (eventId: {}). Skipping.", eventId);
            return;
        }

        try {
            Profile profile = profileService.getProfileEntity(event.userId());
            List<String> photoKeys = profile.getPhotos();

            profilePhotoFacade.deletePhotos(photoKeys);
            profileService.deleteProfile(event.userId());
        } catch (ProfileNotFoundException e) {
            log.warn("Profile for user {} already deleted. Proceeding to save inbox event.", event.userId());
        }

        inboxEventRepository.save(new InboxEvent(eventId));
    }
}