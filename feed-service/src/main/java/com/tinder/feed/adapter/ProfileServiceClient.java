package com.tinder.feed.adapter;

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

    private final ProfileClient profileClient;

    public List<UUID> fetchCandidates(UUID userId) {
        try {
            log.info("Fetching candidates for user {}", userId);

            return profileClient.fetchCandidates(userId);
        } catch (RestClientException e) {
            log.error("Error while fetching candidates for user {}", userId, e);
            return Collections.emptyList();
        }
    }
}
