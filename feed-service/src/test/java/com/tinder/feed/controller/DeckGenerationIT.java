package com.tinder.feed.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.tinder.feed.dto.ProfileResponse;
import com.tinder.feed.event.ActivityType;
import com.tinder.feed.event.SwipeCreatedEvent;
import com.tinder.feed.event.UserActivityEvent;
import com.tinder.feed.util.IntegrationTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

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
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Deck Generation & Caching System Test")
class DeckGenerationIT extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private static WireMockServer wireMockServer;

    private final UUID TEST_USER_ID = UUID.randomUUID();
    private final UUID CANDIDATE_1 = UUID.randomUUID();
    private final UUID CANDIDATE_2 = UUID.randomUUID();

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("app.services.profile.url", wireMockServer::baseUrl);
    }

    @BeforeEach
    void clean() {
        redisTemplate.getRequiredConnectionFactory().getConnection().serverCommands().flushAll();
        wireMockServer.resetAll();
    }

    @Nested
    @DisplayName("Kafka Event Handling")
    class EventTests {

        @Test
        @DisplayName("LOGIN event should trigger deck generation in Redis")
        void loginEvent_ShouldPopulateRedis() throws JsonProcessingException {
            stubFor(get(urlPathEqualTo("/api/v1/internal/profiles/candidates"))
                    .willReturn(okJson(objectMapper.writeValueAsString(List.of(CANDIDATE_1, CANDIDATE_2)))));

            UserActivityEvent event = new UserActivityEvent(TEST_USER_ID, ActivityType.LOGIN, Instant.now());
            kafkaTemplate.send("user-activity-events", TEST_USER_ID.toString(), objectMapper.writeValueAsString(event));

            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                Long size = redisTemplate.opsForList().size("user:" + TEST_USER_ID + ":deck");
                assertEquals(2, size, "Redis deck should contain 2 candidates");
            });
        }

        @Test
        @DisplayName("LOCATION_UPDATE should clear old deck and generate new")
        void locationUpdate_ShouldRefreshDeck() throws JsonProcessingException {
            String deckKey = "user:" + TEST_USER_ID + ":deck";
            redisTemplate.opsForList().rightPush(deckKey, UUID.randomUUID().toString());

            stubFor(get(urlPathEqualTo("/api/v1/internal/profiles/candidates"))
                    .willReturn(okJson(objectMapper.writeValueAsString(List.of(CANDIDATE_1)))));

            UserActivityEvent event = new UserActivityEvent(TEST_USER_ID, ActivityType.LOCATION_UPDATE, Instant.now());
            kafkaTemplate.send("user-activity-events", TEST_USER_ID.toString(), objectMapper.writeValueAsString(event));

            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                List<String> deck = redisTemplate.opsForList().range(deckKey, 0, -1);
                assertNotNull(deck);
                assertEquals(1, deck.size());
                assertEquals(CANDIDATE_1.toString(), deck.getFirst());
            });
        }
    }

    @Nested
    @DisplayName("API & Caching Strategy")
    class ApiTests {

        @Test
        @DisplayName("Full Cycle: Get Feed with Lazy Loading and HTTP Fallback")
        void getFeed_ShouldUseCacheAndStoreMissingProfiles() throws JsonProcessingException {
            String deckKey = "user:" + TEST_USER_ID + ":deck";
            redisTemplate.opsForList().rightPushAll(deckKey, CANDIDATE_1.toString(), CANDIDATE_2.toString());

            ProfileResponse p1 = new ProfileResponse(CANDIDATE_1, "Ivan", 25, "MALE",
                    "Bio", List.of(), null);
            redisTemplate.opsForValue().set("user:" + CANDIDATE_1 + ":profile", objectMapper.writeValueAsString(p1));

            ProfileResponse p2 = new ProfileResponse(CANDIDATE_2, "Maria", 22, "FEMALE",
                    "Bio", List.of(), null);
            stubFor(post(urlEqualTo("/api/v1/internal/profiles/batch"))
                    .willReturn(okJson(objectMapper.writeValueAsString(List.of(p2)))));

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-User-Id", TEST_USER_ID.toString());
            HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);

            ResponseEntity<List<ProfileResponse>> response = restTemplate.exchange(
                    "/api/v1/feed",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    });

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().size());

            await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
                String cachedP2 = redisTemplate.opsForValue().get("user:" + CANDIDATE_2 + ":profile");
                assertNotNull(cachedP2, "Profile 2 should be lazy-cached in Redis");
            });

            verify(1, postRequestedFor(urlEqualTo("/api/v1/internal/profiles/batch")));
        }

        @Test
        @DisplayName("Swipe history should filter out candidates")
        void swipeEvent_ShouldDeduplicateFeed() throws JsonProcessingException {
            SwipeCreatedEvent swipe = new SwipeCreatedEvent(TEST_USER_ID, CANDIDATE_1, true, Instant.now());
            kafkaTemplate.send("swipe-events", TEST_USER_ID.toString(), objectMapper.writeValueAsString(swipe));

            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                Boolean isSwiped = redisTemplate.opsForSet().isMember("user:" + TEST_USER_ID + ":history", CANDIDATE_1.toString());
                assertTrue(Boolean.TRUE.equals(isSwiped));
            });

            stubFor(get(urlPathEqualTo("/api/v1/internal/profiles/candidates"))
                    .willReturn(okJson(objectMapper.writeValueAsString(List.of(CANDIDATE_1, CANDIDATE_2)))));

            ProfileResponse p2 = new ProfileResponse(CANDIDATE_2, "Maria", 22, "FEMALE",
                    "Bio", List.of(), null);
            stubFor(post(urlEqualTo("/api/v1/internal/profiles/batch"))
                    .willReturn(okJson(objectMapper.writeValueAsString(List.of(p2)))));

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-User-Id", TEST_USER_ID.toString());
            HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);

            ResponseEntity<List<ProfileResponse>> response = restTemplate.exchange(
                    "/api/v1/feed",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    });

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size(), "Feed should contain exactly 1 profile");
            assertEquals(CANDIDATE_2, response.getBody().getFirst().userId(), "Swiped user MUST NOT be in the feed");
        }
    }
}