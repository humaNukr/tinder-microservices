package com.tinder.chat.infrastructure.adapter.out.profile;

import com.tinder.chat.shared.dto.external.ProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileProviderAdapterTest {

    @Mock
    private ProfileCacheClient profileCacheClient;
    @Mock
    private ProfileRestAdapter profileRestAdapter;

    @InjectMocks
    private ProfileProviderAdapter adapter;

    private UUID id1;
    private UUID id2;
    private ProfileResponse profile1;
    private ProfileResponse profile2;

    @BeforeEach
    void setUp() {
        id1 = UUID.randomUUID();
        id2 = UUID.randomUUID();
        profile1 = createProfile(id1, "John");
        profile2 = createProfile(id2, "Jane");
    }

    private ProfileResponse createProfile(UUID id, String name) {
        return new ProfileResponse(
                id,
                name,
                25,
                "OTHER",
                "Some bio",
                List.of("Java", "Spring"),
                List.of("https://photo.com/1.jpg")
        );
    }

    @Nested
    class GetProfilesMap {

        @Test
        void getProfilesMap_EmptyInput_ReturnsEmptyMap() {
            Map<UUID, ProfileResponse> result = adapter.getProfilesMap(Collections.emptySet());

            assertTrue(result.isEmpty());
            verify(profileCacheClient, never()).getCachedProfiles(anyList());
        }

        @Test
        void getProfilesMap_AllProfilesInCache_ReturnsFromCacheAndDoesNotCallRest() {
            Set<UUID> ids = Set.of(id1, id2);
            when(profileCacheClient.getCachedProfiles(anyList())).thenReturn(List.of(profile1, profile2));

            Map<UUID, ProfileResponse> result = adapter.getProfilesMap(ids);

            assertEquals(2, result.size());
            assertEquals(profile1, result.get(id1));
            assertEquals(profile2, result.get(id2));
            verify(profileRestAdapter, never()).batchProfiles(anyList());
        }

        @Test
        void getProfilesMap_PartialCacheMiss_FetchesMissingFromRestAndCachesThem() {
            Set<UUID> ids = Set.of(id1, id2);
            // Тільки id1 є в кеші
            when(profileCacheClient.getCachedProfiles(anyList())).thenReturn(List.of(profile1));
            // id2 довантажуємо через REST
            when(profileRestAdapter.batchProfiles(List.of(id2))).thenReturn(List.of(profile2));

            Map<UUID, ProfileResponse> result = adapter.getProfilesMap(ids);

            assertEquals(2, result.size());
            assertEquals(profile1, result.get(id1));
            assertEquals(profile2, result.get(id2));

            verify(profileRestAdapter).batchProfiles(List.of(id2));
            verify(profileCacheClient).cacheProfiles(List.of(profile2));
        }

        @Test
        void getProfilesMap_FullCacheMiss_FetchesAllFromRest() {
            Set<UUID> ids = Set.of(id1);
            when(profileCacheClient.getCachedProfiles(anyList())).thenReturn(Collections.emptyList());
            when(profileRestAdapter.batchProfiles(anyList())).thenReturn(List.of(profile1));

            Map<UUID, ProfileResponse> result = adapter.getProfilesMap(ids);

            assertEquals(1, result.size());
            assertEquals(profile1, result.get(id1));
            verify(profileCacheClient).cacheProfiles(List.of(profile1));
        }
    }
}