package com.tinder.profile.mapper;

import com.tinder.profile.config.MapperConfig;
import com.tinder.profile.domain.Profile;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.ProfileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.Period;


@Mapper(config = MapperConfig.class)
public interface ProfileMapper {
    @Mapping(target = "age", source = "birthDate", qualifiedByName = "calculateAge")
    ProfileResponse toDto(Profile profile);

    Profile toModel(CreateProfileRequest request);

    @Named("calculateAge")
    default int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
