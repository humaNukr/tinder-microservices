package com.tinder.profile.mapper;

import com.tinder.profile.config.MapperConfig;
import com.tinder.profile.domain.Profile;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.dto.UpdatePreferencesRequest;
import com.tinder.profile.dto.UpdateProfileRequest;
import com.tinder.profile.dto.UserPreferencesResponse;
import com.tinder.profile.properties.MinioProperties;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;


@Mapper(config = MapperConfig.class)
public abstract class ProfileMapper {
    @Autowired
    protected MinioProperties minioProperties;

    @Mapping(target = "age", source = "birthDate", qualifiedByName = "calculateAge")
    @Mapping(target = "photos", qualifiedByName = "buildFullUrls")
    public abstract ProfileResponse toDto(Profile profile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract Profile updateEntityFromDto(UpdateProfileRequest request, @MappingTarget Profile profile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "gender", source = "gender")
    @Mapping(target = "preferences.targetGender", source = "targetGender")
    @Mapping(target = "preferences.minAge", source = "minAge")
    @Mapping(target = "preferences.maxAge", source = "maxAge")
    @Mapping(target = "preferences.maxDistanceKm", source = "maxDistanceKm")
    public abstract Profile updatePreferencesFromDto(UpdatePreferencesRequest request, @MappingTarget Profile profile);

    public abstract Profile toModel(CreateProfileRequest request);

    @Mapping(source = "gender", target = "gender")
    @Mapping(source = "preferences.targetGender", target = "targetGender")
    @Mapping(source = "preferences.minAge", target = "minAge")
    @Mapping(source = "preferences.maxAge", target = "maxAge")
    @Mapping(source = "preferences.maxDistanceKm", target = "maxDistanceKm")
    public abstract UserPreferencesResponse toUserPreferencesResponse(Profile profile);


    @Named("calculateAge")
    protected int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    @Named("buildFullUrls")
    protected List<String> buildFullUrls(List<String> photoKeys) {
        if (photoKeys == null || photoKeys.isEmpty()) {
            return new ArrayList<>();
        }

        String baseUrl = minioProperties.url() + "/";
        return photoKeys.stream().map(key -> baseUrl + key).toList();
    }
}
