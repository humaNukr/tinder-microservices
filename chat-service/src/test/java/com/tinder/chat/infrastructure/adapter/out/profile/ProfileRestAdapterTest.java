package com.tinder.chat.infrastructure.adapter.out.profile;

import com.tinder.chat.shared.dto.external.ProfileResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileRestAdapterTest {

    @Mock
    private ProfileClient profileClient;

    @InjectMocks
    private ProfileRestAdapter adapter;

    @Test
    void batchProfiles_Success_ReturnsFromClient() {
        UUID id = UUID.randomUUID();
        ProfileResponse profile = new ProfileResponse(id, "John", 25, "M", "Bio", List.of(), List.of());
        when(profileClient.batchProfiles(List.of(id))).thenReturn(List.of(profile));

        List<ProfileResponse> result = adapter.batchProfiles(List.of(id));

        assertEquals(1, result.size());
        assertEquals("John", result.get(0).name());
    }

    @Test
    void fallbackBatchProfiles_CalledOnException_ReturnsUnknownUsers() {
        UUID id = UUID.randomUUID();
        Exception ex = new RuntimeException("Service Unavailable");

        List<ProfileResponse> result = ReflectionTestUtils.invokeMethod(
                adapter, "fallbackBatchProfiles", List.of(id), ex);

        Assertions.assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(id, result.getFirst().userId());
        assertEquals("Unknown User", result.get(0).name());
        assertEquals("media/default-avatar.png", result.get(0).photos().get(0));
    }
}