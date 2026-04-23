package com.tinder.feed.adapter;

import com.tinder.feed.dto.ProfileResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.UUID;

public interface ProfileClient {
    @GetExchange("/api/v1/internal/profiles/candidates")
    List<UUID> fetchCandidates(
            @RequestParam("userId") UUID userId,
            @RequestParam("limit") int limit
    );

    @PostExchange("/api/v1/internal/profiles/batch")
    List<ProfileResponse> batchProfiles(@RequestBody List<UUID> userIds);
}