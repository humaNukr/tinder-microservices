package com.tinder.notification.mapper;

import com.tinder.notification.config.MapperConfig;
import com.tinder.notification.dto.SaveTokenRequest;
import com.tinder.notification.entity.DeviceToken;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface DeviceTokenMapper {
    DeviceToken toEntity(SaveTokenRequest requestDto);
}
