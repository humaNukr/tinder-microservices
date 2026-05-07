package com.tinder.profile.mapper;

import com.tinder.profile.config.MapperConfig;
import com.tinder.profile.domain.Gender;
import com.tinder.profile.domain.Profile;
import com.tinder.profile.domain.UserPreferences;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.dto.UpdatePreferencesRequest;
import com.tinder.profile.dto.UpdateProfileRequest;
import com.tinder.profile.dto.UserPreferencesResponse;
import com.tinder.profile.properties.MinioProperties;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;


@Mapper(config = MapperConfig.class)
public abstract class ProfileMapper {

    @Value("${app.media.public-prefix}")
    protected String mediaPrefix;
    @Autowired
    protected MinioProperties minioProperties;

    @Mapping(target = "age", source = "birthDate", qualifiedByName = "calculateAge")
    @Mapping(target = "photos", qualifiedByName = "buildPublicUrls")
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

    @Mapping(target = "preferences.targetGender", source = "targetGender")
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

    @Named("buildPublicUrls")
    protected List<String> buildPublicUrls(List<String> photoKeys) {
        if (photoKeys == null || photoKeys.isEmpty()) {
            return new ArrayList<>();
        }

        return photoKeys.stream().map(key -> {
            String cleanKey = key.replaceFirst("^tinder-media/", "");

            return mediaPrefix + cleanKey;
        }).toList();
    }

    @AfterMapping
    protected void setDefaultPreferences(CreateProfileRequest request, @MappingTarget Profile profile) {
        int userAge = calculateAge(request.birthDate());

        int minAge = Math.max(18, userAge - 3);
        int maxAge = userAge + 5;
        double defaultDistance = 30.0;

        if (profile.getPreferences() != null) {
            profile.getPreferences().setMinAge(minAge);
            profile.getPreferences().setMaxAge(maxAge);
            profile.getPreferences().setMaxDistanceKm(defaultDistance);
        } else {
            UserPreferences prefs = new UserPreferences();
            prefs.setTargetGender(Gender.valueOf(request.targetGender()));
            prefs.setMinAge(minAge);
            prefs.setMaxAge(maxAge);
            prefs.setMaxDistanceKm(defaultDistance);
            profile.setPreferences(prefs);
        }
    }
}
