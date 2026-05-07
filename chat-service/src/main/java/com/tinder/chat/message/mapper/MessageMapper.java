package com.tinder.chat.message.mapper;

import com.tinder.chat.config.MapperConfig;
import com.tinder.chat.infrastructure.redis.contract.MessageDeletedEventDto;
import com.tinder.chat.message.dto.MessageAckDto;
import com.tinder.chat.message.dto.MessageEventDto;
import com.tinder.chat.message.dto.MessageResponseDto;
import com.tinder.chat.message.dto.ReplyInfoDto;
import com.tinder.chat.message.enums.MessageContentType;
import com.tinder.chat.message.model.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Mapper(config = MapperConfig.class)
public interface MessageMapper {

    @Mapping(target = "content", expression = "java(resolveContent(message))")
    MessageEventDto toEventDto(Message message, UUID recipientId);

    @Mapping(target = "messageId", source = "message.id")
    MessageDeletedEventDto toDeletedEventDto(Message message, UUID recipientId);

    @Mapping(target = "content", expression = "java(resolveContent(message))")
    @Mapping(target = "type", source = "contentType")
    MessageResponseDto toResponseDto(Message message);

    List<MessageResponseDto> toResponseDtoList(List<Message> messages);

    @Mapping(target = "messageId", source = "id")
    @Mapping(target = "type", source = "contentType")
    @Mapping(target = "content", expression = "java(resolveContent(message))")
    ReplyInfoDto toReplyInfoDto(Message message);

    @Mapping(target = "dbId", source = "message.id")
    MessageAckDto toAckDto(Message message, UUID localId);


    default String resolveContent(Message message) {
        if (message.isDeleted()) {
            return "TOMBSTONE";
        }

        if (message.getContentType() != MessageContentType.TEXT) {
            String fileNameWithExt = Paths.get(message.getContent()).getFileName().toString();
            return String.format("/api/v1/chats/%s/media/%s", message.getChatId(), fileNameWithExt);
        }

        return message.getContent();
    }
}