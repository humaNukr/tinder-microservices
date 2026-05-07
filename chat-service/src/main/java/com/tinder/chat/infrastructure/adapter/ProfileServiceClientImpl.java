package com.tinder.chat.infrastructure.adapter;

import com.tinder.chat.chat.dto.ProfileResponse;
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
public class ProfileServiceClientImpl implements ProfileServiceClient {

    private final ProfileClient profileClient;

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