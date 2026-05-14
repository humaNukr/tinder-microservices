package com.tinder.chat.infrastructure.adapter.out.persistence;

import com.tinder.chat.application.port.out.message.MessagePersistencePort;
import com.tinder.chat.domain.enums.MessageStatus;
import com.tinder.chat.domain.exception.EntityNotFoundException;
import com.tinder.chat.domain.model.Message;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.MessageJpaEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.mapper.MessageEntityMapper;
import com.tinder.chat.shared.dto.common.CursorPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessagePersistenceAdapter implements MessagePersistencePort {

    private final MessageJpaRepository messageRepository;
    private final MessageEntityMapper mapper;

    @Override
    public Message save(Message message) {
        MessageJpaEntity entityToSave = mapper.toEntity(message);
        MessageJpaEntity savedEntity = messageRepository.save(entityToSave);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Message getById(Long messageId) {
        MessageJpaEntity entity = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));
        return mapper.toDomain(entity);
    }

    @Override
    public Message getByIdWithReactions(Long messageId) {
        MessageJpaEntity entity = messageRepository.findByIdWithReactions(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));
        return mapper.toDomain(entity);
    }

    @Override
    public Message getPendingMessageByObjectKey(String objectKey) {
        MessageJpaEntity entity = messageRepository.findPendingMessageByObjectKey(objectKey)
                .orElseThrow(() -> new EntityNotFoundException("Pending message not found for key: " + objectKey));
        return mapper.toDomain(entity);
    }

    @Override
    public CursorPage<Message> getChatHistoryPage(UUID chatId, Long cursor, int limit) {
        int limitPlusOne = limit + 1;
        Pageable pageable = PageRequest.of(0, limitPlusOne);

        List<MessageJpaEntity> entityList;

        if (cursor == null) {
            entityList = messageRepository.findByChatIdAndStatusNotOrderByIdDesc(
                    chatId, MessageStatus.UPLOADING, pageable);
        } else {
            entityList = messageRepository.findByChatIdAndStatusNotAndIdLessThanOrderByIdDesc(
                    chatId, MessageStatus.UPLOADING, cursor, pageable);
        }

        List<MessageJpaEntity> entities = new ArrayList<>(entityList);

        boolean hasNext = entities.size() > limit;
        if (hasNext) {
            entities.remove(limit);
        }

        Long nextCursor = entities.isEmpty() ? null : entities.getLast().getId();

        // Мапимо список Entity в список Доменних моделей
        List<Message> messages = entities.stream()
                .map(mapper::toDomain)
                .toList();

        return new CursorPage<>(messages, nextCursor, hasNext);
    }
}