package com.tinder.feed.service.interfaces;

import com.tinder.feed.dto.ProfileResponse;

import java.util.List;
import java.util.UUID;

public interface DeckProvider {
    List<ProfileResponse> getFeedForUser(UUID userId);
}
