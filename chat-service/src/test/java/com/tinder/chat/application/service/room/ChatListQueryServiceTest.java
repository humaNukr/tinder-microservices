package com.tinder.chat.application.service.room;

import com.tinder.chat.application.port.out.presence.UserPresencePort;
import com.tinder.chat.application.port.out.profile.ProfilePort;
import com.tinder.chat.application.port.out.room.ChatPersistencePort;
import com.tinder.chat.domain.model.ChatPreview;
import com.tinder.chat.shared.dto.external.ProfileResponse;
import com.tinder.chat.shared.dto.room.ChatListItemDto;
import com.tinder.chat.shared.mapper.ChatMapper;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatListQueryServiceTest {

    @Mock
    private ChatPersistencePort chatPersistencePort;
    @Mock
    private ProfilePort profilePort;
    @Mock
    private UserPresencePort userPresencePort;
    @Mock
    private ChatMapper chatMapper;

    @InjectMocks
    private ChatListQueryService chatListQueryService;

    private UUID myUserId;
    private UUID partnerId;
    private int page;
    private int size;

    @BeforeEach
    void setUp() {
        myUserId = UUID.randomUUID();
        partnerId = UUID.randomUUID();
        page = 0;
        size = 10;
    }

    @Nested
    class GetChatsList {

        @Test
        void getChatsList_EmptyPreviews_ReturnsEmptyList() {
            when(chatPersistencePort.findChatPreviewsByUserId(myUserId, size, 0)).thenReturn(Collections.emptyList());

            List<ChatListItemDto> result = chatListQueryService.getChatsList(myUserId, page, size);

            assertTrue(result.isEmpty());
            verifyNoInteractions(profilePort, userPresencePort, chatMapper);
        }

        @Test
        void getChatsList_ValidPreviews_ReturnsMappedList() {
            ChatPreview preview = mock(ChatPreview.class);
            ProfileResponse profileResponse = mock(ProfileResponse.class);
            ChatListItemDto expectedDto = mock(ChatListItemDto.class);

            when(preview.partnerId()).thenReturn(partnerId);
            when(chatPersistencePort.findChatPreviewsByUserId(myUserId, size, 0)).thenReturn(List.of(preview));

            Set<UUID> partnerIds = Set.of(partnerId);
            when(profilePort.getProfilesMap(partnerIds)).thenReturn(Map.of(partnerId, profileResponse));
            when(userPresencePort.getPresenceBatch(partnerIds)).thenReturn(Map.of(partnerId, true));

            when(chatMapper.toListItemDto(preview, profileResponse, true, myUserId)).thenReturn(expectedDto);

            List<ChatListItemDto> result = chatListQueryService.getChatsList(myUserId, page, size);

            assertEquals(1, result.size());
            assertEquals(expectedDto, result.getFirst());
            verify(chatPersistencePort).findChatPreviewsByUserId(myUserId, size, 0);
        }
    }
}