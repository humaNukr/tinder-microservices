package com.tinder.chat.infrastructure.adapter.out.http;

import com.tinder.chat.shared.dto.external.ProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileRestAdapter {

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