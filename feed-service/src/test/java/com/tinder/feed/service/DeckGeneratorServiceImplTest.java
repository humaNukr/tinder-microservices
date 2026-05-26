package com.tinder.feed.service;

import com.tinder.feed.exception.DeckGenerationInProgressException;
import com.tinder.feed.properties.FeedProperties;
import com.tinder.feed.service.impl.DeckGeneratorServiceImpl;
import com.tinder.feed.service.interfaces.FeedStorageService;
import com.tinder.feed.service.interfaces.ProfileProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeckGeneratorServiceImpl")
class DeckGeneratorServiceImplTest {

    @Mock private ProfileProvider profileProvider;
    @Mock private FeedStorageService storageService;
    @Mock private FeedProperties feedProperties;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private DeckGeneratorServiceImpl generator;

    private UUID userId;
    private UUID c1, c2, c3;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        c1 = UUID.randomUUID();
        c2 = UUID.randomUUID();
        c3 = UUID.randomUUID();
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private void acquireLock() {
        when(valueOps.setIfAbsent(eq(lockKey()), eq("LOCKED"), any(Duration.class))).thenReturn(true);
    }

    private void lockAlreadyHeld() {
        when(valueOps.setIfAbsent(eq(lockKey()), eq("LOCKED"), any(Duration.class))).thenReturn(false);
    }

    private String lockKey() {
        return "lock:generate-deck:" + userId;
    }

    @Nested
    @DisplayName("Distributed lock behaviour")
    class LockBehaviour {

        @Test
        @DisplayName("throws DeckGenerationInProgressException immediately when lock is held")
        void lockHeld_ThrowsException_NoProfileFetch() {
            lockAlreadyHeld();

            assertThrows(DeckGenerationInProgressException.class, () -> generator.generateDeck(userId));

            verify(profileProvider, never()).fetchCandidates(any());
            verify(storageService, never()).saveNewDeck(any(), any());
        }

        @Test
        @DisplayName("releases lock in finally block even when profile fetch throws")
        void profileFetchThrows_LockAlwaysReleased() {
            acquireLock();
            when(profileProvider.fetchCandidates(userId)).thenThrow(new RuntimeException("timeout"));

            try { generator.generateDeck(userId); } catch (Exception ignored) {}

            verify(stringRedisTemplate).delete(lockKey());
        }

        @Test
        @DisplayName("releases lock after successful generation")
        void successfulGeneration_LockReleased() {
            acquireLock();
            when(feedProperties.deckSize()).thenReturn(10);
            when(profileProvider.fetchCandidates(userId)).thenReturn(List.of(c1));
            when(storageService.fetchSwipedUsers(userId)).thenReturn(Set.of());

            generator.generateDeck(userId);

            verify(stringRedisTemplate).delete(lockKey());
        }

        @Test
        @DisplayName("lock key is scoped to userId to allow concurrent generation for different users")
        void lockKey_IsScopedToUserId() {
            acquireLock();
            when(feedProperties.deckSize()).thenReturn(10);
            when(profileProvider.fetchCandidates(userId)).thenReturn(List.of());
            when(storageService.fetchSwipedUsers(userId)).thenReturn(Set.of());

            generator.generateDeck(userId);

            verify(valueOps).setIfAbsent(eq(lockKey()), eq("LOCKED"), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("Candidate filtering logic")
    class FilteringLogic {

        @BeforeEach
        void acquireTheLock() {
            acquireLock();
            when(feedProperties.deckSize()).thenReturn(10);
        }

        @Test
        @DisplayName("saves all candidates when swipe history is empty")
        void emptyHistory_SavesAllCandidates() {
            when(profileProvider.fetchCandidates(userId)).thenReturn(List.of(c1, c2));
            when(storageService.fetchSwipedUsers(userId)).thenReturn(Set.of());

            generator.generateDeck(userId);

            ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
            verify(storageService).saveNewDeck(eq(userId), captor.capture());
            assertEquals(List.of(c1, c2), captor.getValue());
        }

        @Test
        @DisplayName("removes already-swiped candidates before saving deck")
        void swipedCandidatesFiltered_SavesOnlyUnseen() {
            when(profileProvider.fetchCandidates(userId)).thenReturn(List.of(c1, c2, c3));
            when(storageService.fetchSwipedUsers(userId)).thenReturn(Set.of(c1, c3));

            generator.generateDeck(userId);

            ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
            verify(storageService).saveNewDeck(eq(userId), captor.capture());

            List<UUID> saved = captor.getValue();
            assertAll(
                    () -> assertEquals(1, saved.size()),
                    () -> assertTrue(saved.contains(c2)),
                    () -> assertTrue(saved.stream().noneMatch(id -> id.equals(c1) || id.equals(c3)))
            );
        }

        @Test
        @DisplayName("truncates to deckSize when candidates exceed configured limit")
        void candidatesExceedDeckSize_TruncatesCorrectly() {
            when(feedProperties.deckSize()).thenReturn(2);
            when(profileProvider.fetchCandidates(userId)).thenReturn(List.of(c1, c2, c3));
            when(storageService.fetchSwipedUsers(userId)).thenReturn(Set.of());

            generator.generateDeck(userId);

            ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
            verify(storageService).saveNewDeck(eq(userId), captor.capture());
            assertEquals(2, captor.getValue().size());
        }

        @Test
        @DisplayName("calls saveNewDeck with empty list when all candidates are already swiped")
        void allCandidatesSwiped_SavesEmptyDeck() {
            when(profileProvider.fetchCandidates(userId)).thenReturn(List.of(c1, c2));
            when(storageService.fetchSwipedUsers(userId)).thenReturn(Set.of(c1, c2));

            generator.generateDeck(userId);

            verify(storageService).saveNewDeck(eq(userId), eq(List.of()));
        }
    }
}