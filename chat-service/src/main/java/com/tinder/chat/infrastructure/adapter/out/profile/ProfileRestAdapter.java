package com.tinder.chat.infrastructure.adapter.out.profile;

import com.tinder.chat.shared.dto.external.ProfileResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
class ProfileRestAdapter {

    private final ProfileClient profileClient;

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