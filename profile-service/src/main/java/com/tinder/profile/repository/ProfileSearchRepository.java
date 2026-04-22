package com.tinder.profile.repository;

import com.tinder.profile.domain.Gender;
import com.tinder.profile.dto.ProfileCandidateDto;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.LocalDate;
import java.util.List;

public interface ProfileSearchRepository {
    List<ProfileCandidateDto> findCandidates(
            Gender targetGender,
            LocalDate minBirthDate,
            LocalDate maxBirthDate,
            GeoJsonPoint location,
            Double maxDistanceKm,
            List<String> userInterests,
            int limit
    );
}