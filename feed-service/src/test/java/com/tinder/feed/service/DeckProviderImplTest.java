package com.tinder.feed.service;

import com.tinder.feed.dto.ProfileResponse;
import com.tinder.feed.properties.FeedProperties;
import com.tinder.feed.service.impl.AsyncDeckGenerator;
import com.tinder.feed.service.impl.DeckProviderImpl;
import com.tinder.feed.service.interfaces.DeckGeneratorService;
import com.tinder.feed.service.interfaces.FeedStorageService;
import com.tinder.feed.service.interfaces.ProfileProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeckProviderImpl")
class DeckProviderImplTest {

    @Mock private DeckGeneratorService deckGeneratorService;
    @Mock private AsyncDeckGenerator asyncDeckGenerator;
    @Mock private FeedStorageService storageService;
    @Mock private ProfileProvider profileProvider;
    @Mock private FeedProperties feedProperties;

    @InjectMocks private DeckProviderImpl deckProvider;

    private UUID userId;
    private UUID c1;
    private UUID c2;
    private final int PAGE_SIZE = 10;
    private final int REFILL_THRESHOLD = 5;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        c1 = UUID.randomUUID();
        c2 = UUID.randomUUID();
        when(feedProperties.pageSize()).thenReturn(PAGE_SIZE);
    }

    private ProfileResponse profile(UUID id) {
        return new ProfileResponse(id, "Name", 25, "MALE", "Bio", List.of(), List.of());
    }

    @Nested
    @DisplayName("Deck exists in Cache")
    class DeckExists {

        @BeforeEach
        void setupRefillThreshold() {
            when(feedProperties.refillThreshold()).thenReturn(REFILL_THRESHOLD);
        }

        @Test
        @DisplayName("Returns full cached batch - no HTTP profile calls")
        void allCached_ReturnsCacheOnly_NoHttp() {
            List<UUID> deck = List.of(c1, c2);
            List<ProfileResponse> cached = List.of(profile(c1), profile(c2));

            when(storageService.fetchDeckForUser(userId, PAGE_SIZE)).thenReturn(deck);
            when(storageService.getDeckSize(userId)).thenReturn((long) REFILL_THRESHOLD + 1);
            when(storageService.getCachedProfiles(deck)).thenReturn(cached);

            List<ProfileResponse> result = deckProvider.getFeedForUser(userId);

            assertEquals(2, result.size());
            verify(profileProvider, never()).batchProfiles(anyList());
            verify(asyncDeckGenerator, never()).generateDeckAsync(any());
        }

        @Test
        @DisplayName("Partially cached batch fetches missing IDs and caches them")
        void partialCacheHit_FetchesMissingAndCaches() {
            List<UUID> deck = List.of(c1, c2);
            List<ProfileResponse> cached = new ArrayList<>(List.of(profile(c1)));
            ProfileResponse fetched = profile(c2);

            when(storageService.fetchDeckForUser(userId, PAGE_SIZE)).thenReturn(deck);
            when(storageService.getDeckSize(userId)).thenReturn(10L);
            when(storageService.getCachedProfiles(deck)).thenReturn(cached);
            when(profileProvider.batchProfiles(List.of(c2))).thenReturn(List.of(fetched));

            List<ProfileResponse> result = deckProvider.getFeedForUser(userId);

            assertAll(
                    () -> assertEquals(2, result.size()),
                    () -> verify(profileProvider).batchProfiles(List.of(c2)),
                    () -> verify(storageService).cacheProfiles(List.of(fetched))
            );
        }

        @Test
        @DisplayName("Completely empty cache falls back to HTTP batch without caching")
        void noCacheHit_FallsBackToHttp() {
            List<UUID> deck = List.of(c1, c2);

            when(storageService.fetchDeckForUser(userId, PAGE_SIZE)).thenReturn(deck);
            when(storageService.getDeckSize(userId)).thenReturn(10L);
            when(storageService.getCachedProfiles(deck)).thenReturn(Collections.emptyList());
            when(profileProvider.batchProfiles(deck)).thenReturn(List.of(profile(c1), profile(c2)));

            List<ProfileResponse> result = deckProvider.getFeedForUser(userId);

            assertEquals(2, result.size());
            verify(storageService, never()).cacheProfiles(anyList());
        }

        @Test
        @DisplayName("Triggers background async deck generation if remaining items are low")
        void belowThreshold_TriggersAsyncRefill() {
            List<UUID> deck = List.of(c1);

            when(storageService.fetchDeckForUser(userId, PAGE_SIZE)).thenReturn(deck);
            when(storageService.getDeckSize(userId)).thenReturn((long) REFILL_THRESHOLD - 1);
            when(storageService.getCachedProfiles(deck)).thenReturn(List.of(profile(c1)));

            deckProvider.getFeedForUser(userId);

            verify(asyncDeckGenerator).generateDeckAsync(userId);
            verify(deckGeneratorService, never()).generateDeck(any());
        }
    }

    @Nested
    @DisplayName("Deck missing from Cache")
    class DeckMissing {

        @Test
        @DisplayName("Generates deck synchronously on cache miss and refetches")
        void cacheMiss_GeneratesSyncAndReturnsFeed() {
            List<UUID> generatedDeck = List.of(c1, c2);

            when(storageService.fetchDeckForUser(userId, PAGE_SIZE))
                    .thenReturn(Collections.emptyList())
                    .thenReturn(generatedDeck);
            when(storageService.getCachedProfiles(generatedDeck)).thenReturn(List.of(profile(c1), profile(c2)));

            List<ProfileResponse> result = deckProvider.getFeedForUser(userId);

            assertAll(
                    () -> assertEquals(2, result.size()),
                    () -> verify(deckGeneratorService).generateDeck(userId),
                    () -> verify(asyncDeckGenerator, never()).generateDeckAsync(any())
            );
        }

        @Test
        @DisplayName("Returns empty feed immediately if synchronous generation yields no profiles")
        void generationFailsToFindCandidates_ReturnsEmptyList() {
            when(storageService.fetchDeckForUser(userId, PAGE_SIZE)).thenReturn(Collections.emptyList());

            List<ProfileResponse> result = deckProvider.getFeedForUser(userId);

            assertAll(
                    () -> assertTrue(result.isEmpty()),
                    () -> verify(deckGeneratorService).generateDeck(userId),
                    () -> verify(storageService, never()).getCachedProfiles(anyList())
            );
        }
    }
}