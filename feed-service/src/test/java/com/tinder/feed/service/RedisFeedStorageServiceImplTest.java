package com.tinder.feed.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.feed.dto.ProfileResponse;
import com.tinder.feed.service.impl.RedisFeedStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisFeedStorageServiceImpl")
class RedisFeedStorageServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RedisOperations<String, String> pipelinedOperations;
    @Mock
    private ListOperations<String, String> listOps;
    @Mock
    private SetOperations<String, String> setOps;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private RedisFeedStorageServiceImpl redisService;

    private UUID userId;
    private UUID candidateId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
    }

    private String deckKey() {
        return "user:" + userId + ":deck";
    }

    private String historyKey() {
        return "user:" + userId + ":history";
    }

    private String profileKey(UUID id) {
        return "user:" + id + ":profile";
    }

    private ProfileResponse profile(UUID id) {
        return new ProfileResponse(id, "Test", 25, "MALE", "Bio", List.of(), List.of());
    }

    @SuppressWarnings("unchecked")
    private void mockPipelineExecution() {
        when(redisTemplate.executePipelined(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            callback.execute(pipelinedOperations);
            return Collections.emptyList();
        });
    }

    @Nested
    @DisplayName("fetchSwipedUsers()")
    class FetchSwipedUsers {

        @Test
        @DisplayName("Returns set of parsed UUIDs from history set")
        void membersExist_ReturnsUUIDs() {
            UUID swiped1 = UUID.randomUUID();
            UUID swiped2 = UUID.randomUUID();
            when(redisTemplate.opsForSet()).thenReturn(setOps);
            when(setOps.members(historyKey())).thenReturn(Set.of(swiped1.toString(), swiped2.toString()));

            Set<UUID> result = redisService.fetchSwipedUsers(userId);

            assertAll(
                    () -> assertEquals(2, result.size()),
                    () -> assertTrue(result.contains(swiped1)),
                    () -> assertTrue(result.contains(swiped2))
            );
        }

        @Test
        @DisplayName("Returns empty set when history is empty or null")
        void redisReturnsNull_ReturnsEmptySet() {
            when(redisTemplate.opsForSet()).thenReturn(setOps);
            when(setOps.members(historyKey())).thenReturn(null);

            Set<UUID> result = redisService.fetchSwipedUsers(userId);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("saveNewDeck()")
    class SaveNewDeck {

        @Test
        @DisplayName("Replaces deck via pipelined transaction with correct TTL")
        void validCandidates_DeletesAndPushesInPipeline() {
            mockPipelineExecution();
            when(pipelinedOperations.opsForList()).thenReturn(listOps);
            List<UUID> candidates = List.of(candidateId);

            redisService.saveNewDeck(userId, candidates);

            verify(pipelinedOperations).delete(deckKey());
            verify(listOps).rightPushAll(deckKey(), List.of(candidateId.toString()));
            verify(pipelinedOperations).expire(deckKey(), Duration.ofDays(3));
        }

        @Test
        @DisplayName("Aborts silently if candidate list is empty or null")
        void emptyCandidates_NoRedisWrite() {
            redisService.saveNewDeck(userId, List.of());
            redisService.saveNewDeck(userId, null);

            verify(redisTemplate, never()).executePipelined(any(SessionCallback.class));
        }
    }

    @Nested
    @DisplayName("fetchDeckForUser()")
    class FetchDeckForUser {

        @Test
        @DisplayName("Pops limits elements and returns mapped UUIDs")
        void deckExists_ReturnsPoppedUUIDs() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            when(redisTemplate.opsForList()).thenReturn(listOps);
            when(listOps.leftPop(deckKey(), 10)).thenReturn(List.of(id1.toString(), id2.toString()));

            List<UUID> result = redisService.fetchDeckForUser(userId, 10);

            assertAll(
                    () -> assertEquals(2, result.size()),
                    () -> assertEquals(id1, result.get(0)),
                    () -> assertEquals(id2, result.get(1))
            );
        }
    }

    @Nested
    @DisplayName("cacheProfiles()")
    class CacheProfiles {

        @Test
        @DisplayName("Caches profiles in pipeline with 1-day TTL")
        void validProfiles_SerializesAndSetsInPipeline() throws JsonProcessingException {
            mockPipelineExecution();
            when(pipelinedOperations.opsForValue()).thenReturn(valueOps);

            ProfileResponse p = profile(candidateId);
            String json = "{\"userId\":\"" + candidateId + "\"}";
            when(objectMapper.writeValueAsString(p)).thenReturn(json);

            redisService.cacheProfiles(List.of(p));

            verify(valueOps).set(profileKey(candidateId), json, Duration.ofDays(1));
        }

        @Test
        @DisplayName("Continues caching remaining profiles if one fails serialization")
        void partialSerializationFailure_CachesRemaining() throws JsonProcessingException {
            mockPipelineExecution();
            when(pipelinedOperations.opsForValue()).thenReturn(valueOps);

            UUID badId = UUID.randomUUID();
            ProfileResponse goodProfile = profile(candidateId);
            ProfileResponse badProfile = profile(badId);

            when(objectMapper.writeValueAsString(goodProfile)).thenReturn("valid");
            when(objectMapper.writeValueAsString(badProfile)).thenThrow(new JsonProcessingException("error") {
            });

            redisService.cacheProfiles(List.of(goodProfile, badProfile));

            verify(valueOps).set(profileKey(candidateId), "valid", Duration.ofDays(1));
            verify(valueOps, never()).set(eq(profileKey(badId)), anyString(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("getCachedProfiles()")
    class GetCachedProfiles {

        @Test
        @DisplayName("Returns correctly mapped profiles, ignoring null misses")
        void partialCacheMiss_SkipsNulls() throws JsonProcessingException {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            ProfileResponse p1 = profile(id1);
            String json1 = "{\"ok\":true}";

            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.multiGet(List.of(profileKey(id1), profileKey(id2)))).thenReturn(Arrays.asList(json1, null));
            when(objectMapper.readValue(json1, ProfileResponse.class)).thenReturn(p1);

            List<ProfileResponse> result = redisService.getCachedProfiles(List.of(id1, id2));

            assertEquals(1, result.size());
            assertEquals(id1, result.get(0).userId());
        }
    }
}