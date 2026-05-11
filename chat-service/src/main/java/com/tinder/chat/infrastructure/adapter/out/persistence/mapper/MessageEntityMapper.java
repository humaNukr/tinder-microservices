package com.tinder.chat.infrastructure.adapter.out.persistence.mapper;

import com.tinder.chat.domain.model.Message;
import com.tinder.chat.domain.model.MessageReaction;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.MessageJpaEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.MessageReactionJpaEntity;
import com.tinder.chat.infrastructure.config.MapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface MessageEntityMapper {

    Message toDomain(MessageJpaEntity entity);

    MessageJpaEntity toEntity(Message domain);

    MessageReaction toDomainReaction(MessageReactionJpaEntity entity);

    MessageReactionJpaEntity toEntityReaction(MessageReaction domain);
}