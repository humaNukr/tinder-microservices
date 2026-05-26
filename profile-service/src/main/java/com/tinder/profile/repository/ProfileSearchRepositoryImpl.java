package com.tinder.profile.repository;

import com.tinder.profile.domain.Gender;
import com.tinder.profile.dto.ProfileCandidateDto;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.GeoNearOperation;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProfileSearchRepositoryImpl implements ProfileSearchRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<ProfileCandidateDto> findCandidates(
            Gender targetGender, LocalDate minBirthDate, LocalDate maxBirthDate,
            GeoJsonPoint location, Double maxDistanceKm, List<String> userInterests, int limit,
            Set<UUID> excludeUserIds
    ) {

        NearQuery nearQuery = NearQuery.near(location)
                .spherical(true)
                .maxDistance(maxDistanceKm * 1000.0);

        GeoNearOperation geoNear = Aggregation.geoNear(nearQuery, "calculatedDistance");

        Criteria matchCriteria = Criteria.where("gender").is(targetGender)
                .and("birthDate").gte(minBirthDate).lte(maxBirthDate);
        if (excludeUserIds != null && !excludeUserIds.isEmpty()) {
            matchCriteria.and("userId").nin(excludeUserIds);
        }

        AggregationOperation match = Aggregation.match(matchCriteria);

        AggregationOperation normalizeInterests = context -> new Document("$addFields",
                new Document("interestsSafe", new Document("$ifNull", List.of("$interests", Collections.emptyList()))));

        List<String> safeSearcherInterests = userInterests == null ? List.of() : userInterests;

        AggregationOperation addScore = context -> {
            Document distancePenalty = new Document("$divide", List.of("$calculatedDistance", 2000));
            Document matchScore;
            if (safeSearcherInterests.isEmpty()) {
                matchScore = new Document("$subtract", List.of(0, distancePenalty));
            } else {
                Document intersection = new Document("$setIntersection",
                        List.of("$interestsSafe", safeSearcherInterests));
                Document sharedCount = new Document("$size",
                        new Document("$ifNull", List.of(intersection, Collections.emptyList())));
                Document interestBoost = new Document("$multiply", List.of(sharedCount, 10));
                matchScore = new Document("$subtract", List.of(interestBoost, distancePenalty));
            }
            return new Document("$addFields", new Document("matchScore", matchScore));
        };

        AggregationOperation sort = Aggregation.sort(Sort.by(Sort.Direction.DESC, "matchScore"));
        AggregationOperation limitOp = Aggregation.limit(limit);

        AggregationOperation project = Aggregation.project("userId");

        Aggregation aggregation = Aggregation.newAggregation(
                geoNear, match, normalizeInterests, addScore, sort, limitOp, project
        );

        return mongoTemplate.aggregate(aggregation, "profiles", ProfileCandidateDto.class).getMappedResults();
    }
}