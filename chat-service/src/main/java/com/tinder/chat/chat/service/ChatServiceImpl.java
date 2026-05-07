package com.tinder.chat.chat.service;

import com.tinder.chat.chat.dto.ChatListItemDto;
import com.tinder.chat.chat.dto.ProfileResponse;
import com.tinder.chat.chat.model.Chat;
import com.tinder.chat.chat.model.ChatParticipant;
import com.tinder.chat.chat.port.ChatParticipantProvider;
import com.tinder.chat.chat.repository.ChatRepository;
import com.tinder.chat.chat.repository.projection.ChatPreviewProjection;
import com.tinder.chat.infrastructure.provider.ProfileProvider;
import com.tinder.chat.user.service.UserPresenceService;
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
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantProvider participantProvider;
    private final ProfileProvider profileProvider;
    private final UserPresenceService userPresenceService;

    @Override
    public void createChat(UUID user1Id, UUID user2Id) {

        Chat chat = Chat.createNewChat(user1Id, user2Id);

        ChatParticipant participant1 = new ChatParticipant(chat, user1Id);

        ChatParticipant participant2 = new ChatParticipant(chat, user2Id);

        chat.addParticipant(participant1);
        chat.addParticipant(participant2);

        Chat savedChat = chatRepository.save(chat);
        participantProvider.saveParticipants(savedChat.getId(), user1Id, user2Id);
    }

    @Override
    public List<ChatListItemDto> getChatsList(UUID myUserId, int page, int size) {
        int offset = page * size;

        List<ChatPreviewProjection> projections = chatRepository.findChatPreviewsByUserId(myUserId, size, offset);
        if (projections.isEmpty()) return Collections.emptyList();

        Set<UUID> partnerIds = projections.stream()
                .map(ChatPreviewProjection::getPartnerId)
                .collect(Collectors.toSet());

        Map<UUID, ProfileResponse> profilesMap = profileProvider.getProfilesMap(partnerIds);

        Map<UUID, Boolean> presenceMap = userPresenceService.getPresenceBatch(partnerIds);

        return projections.stream().map(p -> {
            ProfileResponse profile = profilesMap.get(p.getPartnerId());

            String avatarUrl = (profile != null && profile.photos() != null && !profile.photos().isEmpty())
                    ? profile.photos().getFirst()
                    : null;

            String name = profile != null ? profile.name() : "Deleted User";

            return new ChatListItemDto(
                    p.getChatId(),
                    p.getPartnerId(),
                    name,
                    avatarUrl,
                    presenceMap.getOrDefault(p.getPartnerId(), false),
                    p.getLastMessageContent(),
                    p.getLastMessageType(),
                    p.getLastMessageCreatedAt(),
                    myUserId.equals(p.getLastMessageSenderId()),
                    p.getUnreadCount()
            );
        }).toList();
    }
}
