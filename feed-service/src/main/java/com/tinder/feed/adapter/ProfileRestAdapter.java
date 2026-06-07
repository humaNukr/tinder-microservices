package com.tinder.feed.adapter;

import com.tinder.feed.dto.ProfileResponse;
import com.tinder.feed.properties.FeedProperties;
import com.tinder.feed.service.interfaces.ProfileProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileRestAdapter implements ProfileProvider {

    private final FeedProperties feedProperties;
    private final ProfileClient profileClient;

    @Override
    public List<UUID> fetchCandidates(UUID userId, Collection<UUID> excludeUserIds) {
        try {
            log.info("Fetching candidates for user {}", userId);
            return profileClient.fetchCandidates(userId, feedProperties.fetchLimit(), excludeUserIds);
        } catch (RestClientException e) {
            log.error("Error while fetching candidates for user {}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    @Retry(name = "profileService", fallbackMethod = "fallbackBatchProfiles")
    @CircuitBreaker(name = "profileService", fallbackMethod = "fallbackBatchProfiles")
    public List<ProfileResponse> batchProfiles(List<UUID> ids) {
        log.info("Fetching batch of profiles for ids {}", ids);
        return profileClient.batchProfiles(ids);
    }

    private List<ProfileResponse> fallbackBatchProfiles(List<UUID> ids, Exception e) {
        log.warn("Profile Service is DOWN! Triggering fallback for {} profiles. Reason: {}", ids.size(), e.getMessage());

        return ids.stream()
                .map(id -> new ProfileResponse(
                        id,
                        "Unknown User",
                        0,
                        "UNKNOWN",
                        "Profile is unavailable",
                        Collections.emptyList(),
                        List.of("media/default-avatar.png")
                ))
                .toList();
    }
}