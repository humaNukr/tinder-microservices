package com.tinder.chat.shared.mapper;

import com.tinder.chat.domain.model.ChatPreview;
import com.tinder.chat.infrastructure.config.MapperConfig;
import com.tinder.chat.shared.dto.external.ProfileResponse;
import com.tinder.chat.shared.dto.room.ChatListItemDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(config = MapperConfig.class)
public interface ChatMapper {

    @Mapping(target = "partnerName", expression = "java(getPartnerName(profile))")
    @Mapping(target = "partnerAvatarUrl", expression = "java(getAvatarUrl(profile))")
    @Mapping(target = "isLastMessageMine", expression = "java(currentUserId.equals(preview.lastMessageSenderId()))")
    @Mapping(target = "unreadCount", source = "preview.unreadCount", defaultValue = "0")
    ChatListItemDto toListItemDto(ChatPreview preview, ProfileResponse profile, boolean isPartnerOnline, UUID currentUserId);

    default String getPartnerName(ProfileResponse profile) {
        return (profile != null && profile.name() != null) ? profile.name() : "Deleted User";
    }

    default String getAvatarUrl(ProfileResponse profile) {
        return (profile != null && profile.photos() != null && !profile.photos().isEmpty())
                ? profile.photos().getFirst()
                : null;
    }
}