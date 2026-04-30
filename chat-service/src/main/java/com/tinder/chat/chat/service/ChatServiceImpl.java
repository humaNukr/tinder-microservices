package com.tinder.chat.chat.service;

import com.tinder.chat.chat.model.Chat;
import com.tinder.chat.chat.model.ChatParticipant;
import com.tinder.chat.chat.port.ChatParticipantProvider;
import com.tinder.chat.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantProvider participantProvider;

    @Override
    public void createChat(UUID user1Id, UUID user2Id) {

        UUID firstUser = user1Id.compareTo(user2Id) < 0 ? user1Id : user2Id;
        UUID secondUser = user1Id.compareTo(user2Id) > 0 ? user1Id : user2Id;

        Chat chat = Chat.builder()
                .id(UUID.randomUUID())
                .user1Id(firstUser)
                .user2Id(secondUser)
                .build();

        ChatParticipant participant1 = new ChatParticipant(chat, firstUser);

        ChatParticipant participant2 = new ChatParticipant(chat, secondUser);

        chat.addParticipant(participant1);
        chat.addParticipant(participant2);

        Chat savedChat = chatRepository.save(chat);
        participantProvider.saveParticipants(savedChat.getId(), firstUser, secondUser);
    }
}
