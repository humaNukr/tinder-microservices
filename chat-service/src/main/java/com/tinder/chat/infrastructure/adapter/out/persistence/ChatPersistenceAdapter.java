package com.tinder.chat.infrastructure.adapter.out.persistence;

import com.tinder.chat.application.port.out.room.ChatPersistencePort;
import com.tinder.chat.domain.model.Chat;
import com.tinder.chat.domain.model.ChatPreview;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.ChatJpaEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.mapper.ChatEntityMapper;
import com.tinder.chat.infrastructure.adapter.out.persistence.mapper.ChatPreviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatPersistenceAdapter implements ChatPersistencePort {

    private final ChatJpaRepository repository;
    private final ChatPreviewMapper previewMapper;
    private final ChatEntityMapper chatMapper;

    @Override
    public Chat save(Chat chat) {
        ChatJpaEntity entityToSave = chatMapper.toEntity(chat);
        ChatJpaEntity savedEntity = repository.save(entityToSave);
        return chatMapper.toDomain(savedEntity);
    }

    @Override
    public List<ChatPreview> findChatPreviewsByUserId(UUID userId, int limit, int offset) {
        return repository.findChatPreviewsByUserId(userId, limit, offset)
                .stream()
                .map(previewMapper::toDomain)
                .toList();
    }
}