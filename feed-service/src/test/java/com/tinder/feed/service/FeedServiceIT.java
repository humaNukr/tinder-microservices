package com.tinder.feed.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.feed.dto.ProfileResponse;
import com.tinder.feed.event.ActivityType;
import com.tinder.feed.event.SwipeCreatedEvent;
import com.tinder.feed.event.UserActivityEvent;
import com.tinder.feed.properties.ProfileServiceProperties;
import com.tinder.feed.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Feed Service — Integration Tests")
class FeedServiceIT extends BaseIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProfileServiceProperties profileProps;

    @Value("${app.kafka.topics.swipe-events}")
    private String swipeEventsTopic;

    private UUID userId, c1, c2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        c1 = UUID.randomUUID();
        c2 = UUID.randomUUID();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        wireMock.resetAll();
    }

    private ProfileResponse profile(UUID id, String name) {
        return new ProfileResponse(id, name, 25, "MALE", "Bio", List.of(), List.of());
    }

    private ResponseEntity<List<ProfileResponse>> getFeed(UUID forUser) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", forUser.toString());
        return restTemplate.exchange("/api/v1/feed", HttpMethod.GET, new HttpEntity<>(null, headers), new ParameterizedTypeReference<>() {});
    }

    private void stubCandidates(List<UUID> candidates) throws Exception {
        stubFor(get(urlPathEqualTo(profileProps.candidatesPath()))
                .willReturn(okJson(objectMapper.writeValueAsString(candidates))));
    }

    private void stubBatchProfiles(List<ProfileResponse> profiles) throws Exception {
        stubFor(post(urlEqualTo(profileProps.batchPath()))
                .willReturn(okJson(objectMapper.writeValueAsString(profiles))));
    }

    private void seedDeck(UUID... candidates) {
        for (UUID c : candidates) redisTemplate.opsForList().rightPush("user:" + userId + ":deck", c.toString());
    }

    private void seedCachedProfile(ProfileResponse profile) throws Exception {
        redisTemplate.opsForValue().set("user:" + profile.userId() + ":profile", objectMapper.writeValueAsString(profile));
    }

    private void seedSwipeHistory(UUID... swiped) {
        for (UUID s : swiped) redisTemplate.opsForSet().add("user:" + userId + ":history", s.toString());
    }

    private void publishKafka(String topic, Object event) throws Exception {
        kafkaTemplate.send(topic, userId.toString(), objectMapper.writeValueAsString(event));
    }

    @Nested
    @DisplayName("Cache strategy — GET /api/v1/feed")
    class CacheStrategy {

        @Test
        @DisplayName("serves entire response from Redis — zero HTTP calls")
        void allProfilesCached_NoHttpFallback() throws Exception {
            seedDeck(c1, c2);
            seedCachedProfile(profile(c1, "Ivan"));
            seedCachedProfile(profile(c2, "Maria"));

            ResponseEntity<List<ProfileResponse>> response = getFeed(userId);

            assertAll(
                    () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
                    () -> assertEquals(2, response.getBody().size())
            );
            wireMock.verify(0, postRequestedFor(urlEqualTo(profileProps.batchPath())));
        }

        @Test
        @DisplayName("partial miss lazy-loads missing profiles")
        void partialCacheHit_LazyLoadsAndCachesMissing() throws Exception {
            seedDeck(c1, c2);
            seedCachedProfile(profile(c1, "Ivan"));
            stubBatchProfiles(List.of(profile(c2, "Maria")));

            ResponseEntity<List<ProfileResponse>> response = getFeed(userId);

            assertEquals(2, response.getBody().size());
            await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
                    assertNotNull(redisTemplate.opsForValue().get("user:" + c2 + ":profile"))
            );
            wireMock.verify(1, postRequestedFor(urlEqualTo(profileProps.batchPath())));
        }

        @Test
        @DisplayName("cache miss generates synchronously")
        void noDeckInRedis_GeneratesSyncAndReturnsFeed() throws Exception {
            stubCandidates(List.of(c1, c2));
            stubBatchProfiles(List.of(profile(c1, "Ivan"), profile(c2, "Maria")));

            ResponseEntity<List<ProfileResponse>> response = getFeed(userId);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(2, response.getBody().size());
        }
    }

    @Nested
    @DisplayName("Swipe deduplication — end-to-end")
    class SwipeDeduplication {

        @Test
        @DisplayName("swiped candidate in history is excluded")
        void seededSwipeHistory_FiltersCandidateFromDeck() throws Exception {
            seedSwipeHistory(c1);
            stubCandidates(List.of(c1, c2));
            stubBatchProfiles(List.of(profile(c2, "Maria")));

            ResponseEntity<List<ProfileResponse>> response = getFeed(userId);

            assertAll(
                    () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
                    () -> assertEquals(1, response.getBody().size()),
                    () -> assertEquals(c2, response.getBody().getFirst().userId())
            );
        }

        @Test
        @DisplayName("swipe via Kafka is reflected in next generation")
        void swipeViaKafka_FilteredInSubsequentDeckGeneration() throws Exception {
            publishKafka(swipeEventsTopic, new SwipeCreatedEvent(userId, c1, true, Instant.now()));

            await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                    assertTrue(Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("user:" + userId + ":history", c1.toString())))
            );

            stubCandidates(List.of(c1, c2));
            stubBatchProfiles(List.of(profile(c2, "Maria")));

            ResponseEntity<List<ProfileResponse>> response = getFeed(userId);
            assertEquals(1, response.getBody().size());
            assertEquals(c2, response.getBody().getFirst().userId());
        }
    }
}