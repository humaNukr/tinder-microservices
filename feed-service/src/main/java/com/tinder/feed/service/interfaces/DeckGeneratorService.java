package com.tinder.feed.service.interfaces;

import java.util.UUID;

public interface DeckGeneratorService {
    void generateDeck(UUID userId);

    void generateDeckAsync(UUID userId);
}
