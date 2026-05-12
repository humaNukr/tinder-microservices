package com.tinder.chat.infrastructure.adapter.out.persistence.mapper;

import com.tinder.chat.domain.model.Chat;
import com.tinder.chat.domain.model.ChatParticipant;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.ChatJpaEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.ChatParticipantId;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.ChatParticipantJpaEntity;
import com.tinder.chat.infrastructure.config.MapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(config = MapperConfig.class)
public interface ChatEntityMapper {

    Chat toDomain(ChatJpaEntity entity);

    ChatJpaEntity toEntity(Chat domain);

    @Mapping(target = "userId", source = "id.userId")
    @Mapping(target = "chat", ignore = true)
    ChatParticipant toDomainParticipant(ChatParticipantJpaEntity entity);

    @Mapping(target = "id", source = ".")
    @Mapping(target = "chat", ignore = true)
    ChatParticipantJpaEntity toEntityParticipant(ChatParticipant domain);

    default ChatParticipantId mapParticipantId(ChatParticipant domain) {
        if (domain == null) {
            return null;
        }
        UUID chatId = domain.getChat() != null ? domain.getChat().getId() : null;
        return new ChatParticipantId(chatId, domain.getUserId());
    }
}