package com.tinder.chat.infrastructure.adapter;

import com.tinder.chat.chat.dto.ProfileResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.UUID;

public interface ProfileClient {
    @PostExchange("/api/v1/internal/profiles/batch")
    List<ProfileResponse> batchProfiles(@RequestBody List<UUID> userIds);
}