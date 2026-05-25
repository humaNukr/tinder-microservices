package com.tinder.feed.service.impl;

import com.tinder.feed.service.interfaces.DeckGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsyncDeckGenerator {
    private final DeckGeneratorService deckGeneratorService;

    @Async
    public void generateDeckAsync(UUID userId) {
        try {
            deckGeneratorService.generateDeck(userId);
        } catch (Exception e) {
        }
    }
}