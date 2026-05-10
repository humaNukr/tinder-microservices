package com.tinder.chat.infrastructure.adapter.out.persistence;

import com.tinder.chat.application.port.out.message.MessagePersistencePort;
import com.tinder.chat.domain.enums.MessageStatus;
import com.tinder.chat.domain.exception.EntityNotFoundException;
import com.tinder.chat.domain.model.Message;
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

    @Override
    public Message save(Message message) {
        return messageRepository.save(message);
    }

    @Override
    public Message getById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));
    }

    @Override
    public Message getByIdWithReactions(Long messageId) {
        return messageRepository.findByIdWithReactions(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));
    }

    @Override
    public Message getPendingMessageByObjectKey(String objectKey) {
        return messageRepository.findPendingMessageByObjectKey(objectKey)
                .orElseThrow(() -> new EntityNotFoundException("Pending message not found for key: " + objectKey));
    }

    @Override
    public CursorPage<Message> getChatHistoryPage(UUID chatId, Long cursor, int limit) {
        int limitPlusOne = limit + 1;
        Pageable pageable = PageRequest.of(0, limitPlusOne);

        List<Message> messageList;

        if (cursor == null) {
            messageList = messageRepository.findByChatIdAndStatusNotOrderByIdDesc(
                    chatId, MessageStatus.UPLOADING, pageable);
        } else {
            messageList = messageRepository.findByChatIdAndStatusNotAndIdLessThanOrderByIdDesc(
                    chatId, MessageStatus.UPLOADING, cursor, pageable);
        }

        List<Message> messages = new ArrayList<>(messageList);

        boolean hasNext = messages.size() > limit;
        if (hasNext) {
            messages.remove(limit);
        }

        Long nextCursor = messages.isEmpty() ? null : messages.getLast().getId();

        return new CursorPage<>(messages, nextCursor, hasNext);
    }
}