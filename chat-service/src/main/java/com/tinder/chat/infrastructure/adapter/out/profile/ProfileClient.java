package com.tinder.chat.infrastructure.adapter.out.profile;

import com.tinder.chat.shared.dto.external.ProfileResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.UUID;

interface ProfileClient {
    @PostExchange("/api/v1/internal/profiles/batch")
    List<ProfileResponse> batchProfiles(@RequestBody List<UUID> userIds);
}