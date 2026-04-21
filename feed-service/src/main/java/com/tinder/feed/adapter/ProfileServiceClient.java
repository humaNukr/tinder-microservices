package com.tinder.feed.adapter;

import com.tinder.feed.dto.ProfileResponse;
import com.tinder.feed.properties.FeedProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceClient {

    private final FeedProperties feedProperties;

    private final ProfileClient profileClient;

    public List<UUID> fetchCandidates(UUID userId) {
        try {
            log.info("Fetching candidates for user {}", userId);
            return profileClient.fetchCandidates(userId, feedProperties.fetchLimit());
        } catch (RestClientException e) {
            log.error("Error while fetching candidates for user {}", userId, e);
            return Collections.emptyList();
        }
    }

    public List<ProfileResponse> batchProfiles(List<UUID> ids) {
        try {
            log.info("Fetching batch of profiles for ids {}", ids);
            return profileClient.batchProfiles(ids);
        } catch (RestClientException e) {
            log.error("Error while fetching batch of profiles for ids {}", ids, e);
            return Collections.emptyList();
        }
    }
}
