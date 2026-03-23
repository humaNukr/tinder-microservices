package com.tinder.profile.mapper;

import com.tinder.profile.config.MapperConfig;
import com.tinder.profile.domain.Profile;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.properties.MinioProperties;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
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

    public abstract Profile toModel(CreateProfileRequest request);

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
