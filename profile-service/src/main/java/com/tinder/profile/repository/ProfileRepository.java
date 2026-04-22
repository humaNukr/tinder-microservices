package com.tinder.profile.repository;

import com.tinder.profile.domain.Gender;
import com.tinder.profile.domain.Profile;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends MongoRepository<Profile, String> {
    Optional<Profile> findByUserId(UUID userId);

    List<Profile> findAllByUserIdIn(Collection<UUID> userId);

    boolean existsByUserId(UUID userId);

    List<Profile> findByGenderAndLocationNear(
            Gender gender,
            Point location,
            Distance maxDistance
    );

    List<Profile> findCandidadesForUser(UUID userId);
}