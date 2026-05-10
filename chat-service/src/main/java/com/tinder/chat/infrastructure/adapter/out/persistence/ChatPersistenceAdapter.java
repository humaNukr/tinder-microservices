package com.tinder.chat.infrastructure.adapter.out.persistence;

import com.tinder.chat.application.port.out.room.ChatPersistencePort;
import com.tinder.chat.domain.model.Chat;
import com.tinder.chat.domain.model.ChatPreview;
import com.tinder.chat.shared.mapper.ChatPreviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatPersistenceAdapter implements ChatPersistencePort {

    private final ChatJpaRepository repository;
    private final ChatPreviewMapper mapper;

    @Override
    public Chat save(Chat chat) {
        return repository.save(chat);
    }

    @Override
    public List<ChatPreview> findChatPreviewsByUserId(UUID userId, int limit, int offset) {
        return repository.findChatPreviewsByUserId(userId, limit, offset)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}