package com.tinder.profile.service.impl;

import com.tinder.profile.domain.Profile;
import com.tinder.profile.dto.LocationUpdateRequest;
import com.tinder.profile.event.ActivityType;
import com.tinder.profile.event.ProfileChangedEvent;
import com.tinder.profile.exception.ProfileNotFoundException;
import com.tinder.profile.mapper.ProfileMapper;
import com.tinder.profile.producer.UserActivityProducer;
import com.tinder.profile.repository.ProfileRepository;
import com.tinder.profile.service.interfaces.ProfileLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileLocationServiceImpl implements ProfileLocationService {

    private final MongoTemplate mongoTemplate;
    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UserActivityProducer activityProducer;

    @Override
    public void updateLocation(UUID userId, LocationUpdateRequest request) {
        GeoJsonPoint point = new GeoJsonPoint(request.longitude(), request.latitude());
        LocalDateTime now = LocalDateTime.now();

        Query query = new Query(Criteria.where("userId").is(userId));
        Update update = new Update()
                .set("location", point)
                .set("lastSeen", now);

        var result = mongoTemplate.updateFirst(query, update, Profile.class);
        if (result.getMatchedCount() == 0) {
            throw new ProfileNotFoundException("Profile with id " + userId + " not found");
        }

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile with id " + userId + " not found"));
        
        eventPublisher.publishEvent(new ProfileChangedEvent(profileMapper.toDto(profile)));
        activityProducer.publishActivity(userId, ActivityType.LOCATION_UPDATE);
    }

    @Override
    public void updateLastSeen(UUID userId, Instant timestamp) {
        LocalDateTime lastSeen = LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC);

        Query query = new Query(Criteria.where("userId").is(userId));
        Update update = new Update().max("lastSeen", lastSeen);

        mongoTemplate.updateFirst(query, update, Profile.class);

        log.debug("Attempted to update last_seen for user {} to {}", userId, lastSeen);
    }
}
