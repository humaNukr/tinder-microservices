package com.tinder.profile.service.impl;

import com.tinder.profile.domain.Profile;
import com.tinder.profile.domain.UserPreferences;
import com.tinder.profile.dto.ProfileCandidateDto;
import com.tinder.profile.exception.EmptyOrNullValueException;
import com.tinder.profile.exception.ProfileNotFoundException;
import com.tinder.profile.repository.ProfileRepository;
import com.tinder.profile.repository.ProfileSearchRepository;
import com.tinder.profile.service.interfaces.ProfileFeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileFeedServiceImpl implements ProfileFeedService {

    private final ProfileRepository profileRepository;
    private final ProfileSearchRepository profileSearchRepository;

    @Override
    public List<UUID> getCandidatesForFeed(UUID userId, int limit, Collection<UUID> excludeUserIds) {
        Profile searcher = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile with id " + userId + " not found"));

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
}
