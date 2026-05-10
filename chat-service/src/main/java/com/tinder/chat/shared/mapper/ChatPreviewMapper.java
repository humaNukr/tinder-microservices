package com.tinder.chat.shared.mapper;

import com.tinder.chat.domain.model.ChatPreview;
import com.tinder.chat.infrastructure.adapter.out.persistence.projections.ChatPreviewProjection;
import com.tinder.chat.infrastructure.config.MapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface ChatPreviewMapper {
    ChatPreview toDomain(ChatPreviewProjection projection);
}