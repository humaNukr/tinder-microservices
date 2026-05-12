package com.tinder.chat.application.service.room;

import com.tinder.chat.application.port.in.room.CreateChatUseCase;
import com.tinder.chat.application.port.out.room.ChatParticipantPort;
import com.tinder.chat.application.port.out.room.ChatPersistencePort;
import com.tinder.chat.domain.model.Chat;
import com.tinder.chat.domain.model.ChatParticipant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatCreationService implements CreateChatUseCase {

    private final ChatPersistencePort chatPersistencePort;
    private final ChatParticipantPort chatParticipantPort;

    @Override
    @Transactional
    public void createChat(UUID user1Id, UUID user2Id) {
        Chat chat = Chat.createNewChat(user1Id, user2Id);
        chat.addParticipant(new ChatParticipant(chat, user1Id));
        chat.addParticipant(new ChatParticipant(chat, user2Id));

        Chat savedChat = chatPersistencePort.save(chat);
        chatParticipantPort.saveParticipants(savedChat.getId(), user1Id, user2Id);
    }
}