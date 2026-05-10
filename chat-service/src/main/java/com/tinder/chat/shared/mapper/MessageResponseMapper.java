package com.tinder.chat.shared.mapper;

import com.tinder.chat.domain.model.Message;
import com.tinder.chat.domain.model.MessageReaction;
import com.tinder.chat.infrastructure.config.MapperConfig;
import com.tinder.chat.shared.dto.message.MessageResponseDto;
import com.tinder.chat.shared.dto.message.ReactionInfoDto;
import com.tinder.chat.shared.dto.message.ReplyInfoDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapperConfig.class, uses = MessageContentResolver.class)
public interface MessageResponseMapper {

    @Mapping(target = "type", source = "contentType")
    @Mapping(target = "content", source = ".", qualifiedByName = "resolveContent")
    @Mapping(target = "replyTo", source = "parentMessage")
    MessageResponseDto toResponseDto(Message message);

    List<MessageResponseDto> toResponseDtoList(List<Message> messages);

    @Mapping(target = "messageId", source = "id")
    @Mapping(target = "type", source = "contentType")
    @Mapping(target = "content", source = ".", qualifiedByName = "resolveContent")
    ReplyInfoDto toReplyInfoDto(Message message);

    ReactionInfoDto toReactionInfoDto(MessageReaction reaction);
}