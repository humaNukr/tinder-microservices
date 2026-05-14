package com.tinder.chat.application.service.message;

import com.tinder.chat.application.port.in.message.ToggleReactionUseCase;
import com.tinder.chat.application.port.out.message.MessagePersistencePort;
import com.tinder.chat.application.port.out.notification.ChatEventPort;
import com.tinder.chat.application.service.room.ChatRoomValidator;
import com.tinder.chat.domain.model.Message;
import com.tinder.chat.domain.model.MessageReaction;
import com.tinder.chat.shared.dto.event.ReactionEventDto;
import com.tinder.chat.shared.dto.message.ReactionRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageReactionService implements ToggleReactionUseCase {

    private final MessagePersistencePort persistencePort;
    private final ChatRoomValidator chatRoomValidator;
    private final ChatEventPort eventPort;

    @Override
    @Transactional
    public void toggleReaction(UUID senderId, ReactionRequestDto requestDto) {
        Set<UUID> participants = chatRoomValidator.validateAndGetParticipants(requestDto.chatId(), senderId);
        UUID partnerId = chatRoomValidator.getPartnerId(participants, senderId);

        Message message = persistencePort.getByIdWithReactions(requestDto.messageId());

        if (!message.getChatId().equals(requestDto.chatId())) {
            throw new IllegalArgumentException("Message does not belong to this chat");
        }
        if (message.isDeleted()) {
            throw new IllegalStateException("Cannot react to a deleted message");
        }

        String finalReaction = null;

        Optional<MessageReaction> existingReactionOpt = message.getReactions().stream()
                .filter(r -> r.getUserId().equals(senderId))
                .findFirst();

        if (existingReactionOpt.isPresent()) {
            MessageReaction existingReaction = existingReactionOpt.get();
            if (existingReaction.getReaction().equals(requestDto.reaction())) {
                message.removeReaction(existingReaction);
            } else {
                existingReaction.setReaction(requestDto.reaction());
                finalReaction = requestDto.reaction();
            }
        } else {
            MessageReaction newReaction = MessageReaction.builder()
                    .userId(senderId)
                    .reaction(requestDto.reaction())
                    .build();
            message.addReaction(newReaction);
            finalReaction = requestDto.reaction();
        }

        persistencePort.save(message);

        boolean isRemoved = finalReaction == null;
        ReactionEventDto eventDto = new ReactionEventDto(
                requestDto.chatId(),
                requestDto.messageId(),
                senderId,
                partnerId,
                finalReaction,
                isRemoved
        );

        eventPort.publishReaction(eventDto);
    }
}