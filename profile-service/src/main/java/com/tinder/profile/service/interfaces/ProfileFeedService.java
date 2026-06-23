package com.tinder.profile.service.interfaces;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProfileFeedService {
    List<UUID> getCandidatesForFeed(UUID userId, int limit, Collection<UUID> excludeUserIds);
}
