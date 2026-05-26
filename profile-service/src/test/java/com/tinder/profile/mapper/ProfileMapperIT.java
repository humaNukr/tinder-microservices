package com.tinder.profile.mapper;

import com.tinder.profile.domain.Gender;
import com.tinder.profile.domain.Profile;
import com.tinder.profile.domain.UserPreferences;
import com.tinder.profile.dto.CreateProfileRequest;
import com.tinder.profile.dto.ProfileResponse;
import com.tinder.profile.util.BaseIT;
import com.tinder.profile.util.ProfileAgeUtils;
import com.tinder.profile.util.ProfileTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ProfileMapper — Integration Tests")
class ProfileMapperIT extends BaseIT {

    @Autowired
    private ProfileMapper profileMapper;

    @Nested
    @DisplayName("toDto()")
    class ToDto {

        @Test
        @DisplayName("maps profile fields and calculates age")
        void profile_MapsCorrectly() {
            Profile profile = new Profile();
            profile.setUserId(UUID.randomUUID());
            profile.setName("Sam");
            profile.setBirthDate(LocalDate.now().minusYears(28));
            profile.setGender(Gender.FEMALE);
            profile.setBio("Hello");
            profile.setInterests(List.of("art"));
            profile.setPhotos(List.of("tinder-media/u1/p.jpg"));

            ProfileResponse dto = profileMapper.toDto(profile);

            assertAll(
                    () -> assertEquals("Sam", dto.name()),
                    () -> assertEquals(28, dto.age()),
                    () -> assertEquals(Gender.FEMALE, dto.gender()),
                    () -> assertEquals(1, dto.photos().size())
            );
        }
    }

    @Nested
    @DisplayName("toModel()")
    class ToModel {

        @Test
        @DisplayName("maps create request to profile entity")
        void createRequest_MapsFields() {
            CreateProfileRequest request = ProfileTestFixtures.validCreateRequest();

            Profile profile = profileMapper.toModel(request);

            assertAll(
                    () -> assertEquals("Alex", profile.getName()),
                    () -> assertEquals(request.birthDate(), profile.getBirthDate()),
                    () -> assertEquals(Gender.MALE, profile.getGender())
            );
        }
    }

    @Nested
    @DisplayName("calculateAge")
    class Age {

        @Test
        @DisplayName("matches ProfileAgeUtils")
        void sameAsUtility() {
            LocalDate birthDate = LocalDate.now().minusYears(30);
            assertEquals(ProfileAgeUtils.calculateAge(birthDate), profileMapper.toDto(buildProfile(birthDate)).age());
        }

        private Profile buildProfile(LocalDate birthDate) {
            Profile profile = new Profile();
            profile.setUserId(UUID.randomUUID());
            profile.setName("X");
            profile.setBirthDate(birthDate);
            profile.setGender(Gender.MALE);
            profile.setPreferences(new UserPreferences());
            return profile;
        }
    }
}
