package com.tinder.chat.shared.mapper;

import com.tinder.chat.domain.model.Message;
import com.tinder.chat.infrastructure.config.MapperConfig;
import com.tinder.chat.shared.dto.event.MessageAckDto;
import com.tinder.chat.shared.dto.event.MessageDeletedEventDto;
import com.tinder.chat.shared.dto.event.MessageEditedEventDto;
import com.tinder.chat.shared.dto.event.MessageEventDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(config = MapperConfig.class, uses = MessageContentResolver.class)
public interface MessageEventMapper {

    @Mapping(target = "type", source = "message.contentType")
    @Mapping(target = "content", source = "message", qualifiedByName = "resolveContent")
    @Mapping(target = "replyTo", source = "message.parentMessage")
    MessageEventDto toEventDto(Message message, UUID recipientId);

    @Mapping(target = "messageId", source = "message.id")
    @Mapping(target = "newContent", source = "message.content")
    MessageEditedEventDto toEditedEventDto(Message message, UUID recipientId);

    @Mapping(target = "messageId", source = "message.id")
    MessageDeletedEventDto toDeletedEventDto(Message message, UUID recipientId);

    @Mapping(target = "dbId", source = "message.id")
    MessageAckDto toAckDto(Message message, UUID localId);
}