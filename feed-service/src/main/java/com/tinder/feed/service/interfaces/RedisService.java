package com.tinder.feed.service.interfaces;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RedisService {

    Set<UUID> fetchSwipedUsers(UUID userId);

    void saveNewDeck(UUID userId, List<UUID> batchCandidates);

    List<UUID> fetchDeckForUser(UUID userId);

    Long getDeckSize(UUID userId);

    void addSwipedUserToHistory(UUID swiperId, UUID swipedId);

    void deleteDeck(UUID userId);
}
