package com.tinder.profile.repository;

import com.tinder.profile.domain.Gender;
import com.tinder.profile.dto.ProfileCandidateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.GeoNearOperation;
import org.springframework.data.mongodb.core.aggregation.SetOperators;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProfileSearchRepositoryImpl implements ProfileSearchRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<ProfileCandidateDto> findCandidates(
            Gender targetGender, LocalDate minBirthDate, LocalDate maxBirthDate,
            GeoJsonPoint location, Double maxDistanceKm, List<String> userInterests, int limit
    ) {

        NearQuery nearQuery = NearQuery.near(location)
                .spherical(true)
                .maxDistance(maxDistanceKm * 1000.0);

        GeoNearOperation geoNear = Aggregation.geoNear(nearQuery, "calculatedDistance");

        AggregationOperation match = Aggregation.match(
                Criteria.where("gender").is(targetGender)
                        .and("birthDate").gte(minBirthDate).lte(maxBirthDate)
        );

        AggregationExpression sharedInterestsCount = ArrayOperators.Size.lengthOfArray(
                SetOperators.SetIntersection.arrayAsSet("$interests")
                        .intersects(userInterests.toArray(new String[0]))
        );

        AggregationExpression scoreFormula = ArithmeticOperators.Subtract.valueOf(
                ArithmeticOperators.Multiply.valueOf(sharedInterestsCount).multiplyBy(10)
        ).subtract(
                ArithmeticOperators.Divide.valueOf("$calculatedDistance").divideBy(2000)
        );

        AggregationOperation addFields = Aggregation.addFields()
                .addField("matchScore").withValueOf(scoreFormula)
                .build();

        AggregationOperation sort = Aggregation.sort(Sort.by(Sort.Direction.DESC, "matchScore"));
        AggregationOperation limitOp = Aggregation.limit(limit);

        AggregationOperation project = Aggregation.project("userId");

        Aggregation aggregation = Aggregation.newAggregation(geoNear, match, addFields, sort, limitOp, project);

        return mongoTemplate.aggregate(aggregation, "profiles", ProfileCandidateDto.class).getMappedResults();
    }
}