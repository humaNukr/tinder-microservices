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

        Chat chat = Chat.builder()
                .id(UUID.randomUUID())
                .user1Id(user1Id)
                .user2Id(user2Id)
                .build();

        ChatParticipant participant1 = new ChatParticipant(chat, user1Id);

        ChatParticipant participant2 = new ChatParticipant(chat, user2Id);

        chat.addParticipant(participant1);
        chat.addParticipant(participant2);

        Chat savedChat = chatRepository.save(chat);
        participantProvider.saveParticipants(savedChat.getId(), user1Id, user2Id);
    }
}
