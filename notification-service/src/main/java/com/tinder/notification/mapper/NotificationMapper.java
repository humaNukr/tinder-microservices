package com.tinder.notification.mapper;

import com.tinder.notification.config.MapperConfig;
import com.tinder.notification.dto.NotificationResponseDto;
import com.tinder.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface NotificationMapper {
    NotificationResponseDto toDto(Notification entity);
}