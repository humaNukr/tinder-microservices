package com.tinder.chat.message.mapper;

import com.tinder.chat.config.MapperConfig;
import com.tinder.chat.message.dto.MessageEventDto;
import com.tinder.chat.message.model.Message;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(config = MapperConfig.class)
public interface MessageMapper {
    MessageEventDto toEventDto(Message message, UUID recipientId);
}
