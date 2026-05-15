package com.tinder.chat.infrastructure.adapter.out.persistence;

import com.tinder.chat.application.port.out.room.ChatParticipantPersistencePort;
import com.tinder.chat.infrastructure.adapter.out.persistence.repository.ChatParticipantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatParticipantPersistenceAdapter implements ChatParticipantPersistencePort {

    private final ChatParticipantJpaRepository repository;

    @Override
    public int updateLastReadMessageIdIfGreater(UUID chatId, UUID userId, Long messageId) {
        return repository.updateLastReadMessageIdIfGreater(chatId, userId, messageId);
    }

    @Override
    public Optional<Long> findLastReadMessageId(UUID chatId, UUID userId) {
        return repository.findLastReadMessageId(chatId, userId);
    }
}