package com.tinder.chat.infrastructure.adapter.out.persistence.mapper;

import com.tinder.chat.domain.model.Message;
import com.tinder.chat.domain.model.MessageReaction;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.MessageJpaEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.MessageReactionJpaEntity;
import com.tinder.chat.infrastructure.config.MapperConfig;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfig.class)
public interface MessageEntityMapper {

    Message toDomain(MessageJpaEntity entity);

    MessageJpaEntity toEntity(Message domain);

    @Mapping(target = "message", ignore = true)
    MessageReaction toDomainReaction(MessageReactionJpaEntity entity);

    @Mapping(target = "message", ignore = true)
    MessageReactionJpaEntity toEntityReaction(MessageReaction domain);

    @AfterMapping
    default void linkReactionsToDomainMessage(@MappingTarget Message message) {
        if (message != null && message.getReactions() != null) {
            message.getReactions().forEach(reaction -> reaction.setMessage(message));
        }
    }

    @AfterMapping
    default void linkReactionsToJpaEntity(@MappingTarget MessageJpaEntity entity) {
        if (entity != null && entity.getReactions() != null) {
            entity.getReactions().forEach(reaction -> reaction.setMessage(entity));
        }
    }
}