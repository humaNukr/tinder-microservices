package com.tinder.chat.infrastructure.adapter.out.persistence.mapper;

import com.tinder.chat.domain.model.Chat;
import com.tinder.chat.domain.model.ChatParticipant;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.ChatJpaEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.ChatParticipantJpaEntity;
import com.tinder.chat.infrastructure.config.MapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface ChatEntityMapper {

    Chat toDomain(ChatJpaEntity entity);

    ChatJpaEntity toEntity(Chat domain);

    @Mapping(target = "userId", source = "id.userId")
    @Mapping(target = "chat", ignore = true)
    ChatParticipant toDomainParticipant(ChatParticipantJpaEntity entity);

    @Mapping(target = "id.chatId", source = "chat.id")
    @Mapping(target = "id.userId", source = "userId")
    @Mapping(target = "chat", ignore = true)
    ChatParticipantJpaEntity toEntityParticipant(ChatParticipant domain);
}