package com.tinder.chat.application.service.room;

import com.tinder.chat.application.port.in.room.GetChatListQuery;
import com.tinder.chat.application.port.out.presence.UserPresencePort;
import com.tinder.chat.application.port.out.profile.ProfilePort;
import com.tinder.chat.application.port.out.room.ChatPersistencePort;
import com.tinder.chat.domain.model.ChatPreview;
import com.tinder.chat.shared.dto.external.ProfileResponse;
import com.tinder.chat.shared.dto.room.ChatListItemDto;
import com.tinder.chat.shared.mapper.ChatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatListQueryService implements GetChatListQuery {

    private final ChatPersistencePort chatPersistencePort;
    private final ProfilePort profilePort;
    private final UserPresencePort userPresencePort;
    private final ChatMapper chatMapper;

    @Override
    public List<ChatListItemDto> getChatsList(UUID myUserId, int page, int size) {
        int offset = page * size;

        List<ChatPreview> previews = chatPersistencePort.findChatPreviewsByUserId(myUserId, size, offset);
        if (previews.isEmpty()) return Collections.emptyList();

        Set<UUID> partnerIds = previews.stream()
                .map(ChatPreview::partnerId)
                .collect(Collectors.toSet());

        Map<UUID, ProfileResponse> profilesMap = profilePort.getProfilesMap(partnerIds);
        Map<UUID, Boolean> presenceMap = userPresencePort.getPresenceBatch(partnerIds);

        return previews.stream()
                .map(p -> chatMapper.toListItemDto(
                        p,
                        profilesMap.get(p.partnerId()),
                        presenceMap.getOrDefault(p.partnerId(), false),
                        myUserId
                ))
                .toList();
    }
}