package com.tinder.profile.listener;

import com.tinder.profile.event.ProfileChangedEvent;
import com.tinder.profile.service.interfaces.ProfileCacheService;
import com.tinder.profile.util.ProfileTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileCacheListener")
class ProfileCacheListenerTest {

    @Mock
    private ProfileCacheService cacheService;

    @InjectMocks
    private ProfileCacheListener listener;

    @Test
    @DisplayName("caches profile on ProfileChangedEvent")
    void profileChanged_CachesResponse() {
        UUID userId = UUID.randomUUID();
        var response = ProfileTestFixtures.sampleResponse(userId);

        listener.handleProfileChange(new ProfileChangedEvent(response));

        verify(cacheService).cacheProfile(response);
    }
}
