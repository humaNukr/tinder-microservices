package com.tinder.swipe.service.impl;

import com.tinder.swipe.dto.swipe.SwipeRequestDto;
import com.tinder.swipe.dto.swipe.SwipeResponseDto;
import com.tinder.swipe.dto.swipe.SwipeStatusProjection;
import com.tinder.swipe.event.MatchEvent;
import com.tinder.swipe.event.SwipeCreatedEvent;
import com.tinder.swipe.exception.SwipeAlreadyExistsException;
import com.tinder.swipe.properties.KafkaProperties;
import com.tinder.swipe.repository.SwipeRepository;
import com.tinder.swipe.service.interfaces.OutboxService;
import com.tinder.swipe.service.interfaces.SwipeRateLimiterService;
import com.tinder.swipe.service.interfaces.SwipeSevice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SwipeServiceImpl implements SwipeSevice {
    private final SwipeRepository swipeRepository;
    private final OutboxService outboxService;
    private final KafkaProperties kafkaProperties;
    private final SwipeRateLimiterService rateLimiterService;

    @Override
    @Transactional
    public SwipeResponseDto processSwipe(UUID actorId, SwipeRequestDto requestDto) {
        UUID targetId = requestDto.targetId();
        boolean isLiked = requestDto.isLiked();

        if (actorId.equals(targetId)) {
            throw new IllegalArgumentException("Actor ID and Target ID cannot be the same");
        }

        if (isLiked) {
            rateLimiterService.checkAndIncrementLikeLimit(actorId, false);
        }

        boolean isActorUser1 = isFirstUser(actorId, targetId);

        SwipeStatusProjection projection = isActorUser1
                ? swipeRepository.upsertSwipeByUser1(actorId, targetId, isLiked)
                : swipeRepository.upsertSwipeByUser2(targetId, actorId, isLiked);

        if (projection == null) {
            log.warn("Duplicate swipe attempt by actorId: {} on targetId: {}", actorId, targetId);
            throw new SwipeAlreadyExistsException("You have already swiped this user.");
        }

        outboxService.saveEvent(
                kafkaProperties.swipe(),
                new SwipeCreatedEvent(actorId, targetId, isLiked, Instant.now())
        );

        if (isLiked && Boolean.TRUE.equals(projection.getIsLikedByUser1()) && Boolean.TRUE.equals(projection.getIsLikedByUser2())) {

            UUID matchEventId = generateDeterministicMatchId(actorId, targetId);

            UUID user1Id = isActorUser1 ? actorId : targetId;
            UUID user2Id = isActorUser1 ? targetId : actorId;

            log.info("Match found! Creating chat between user1Id: {} and user2Id: {}. MatchEventId: {}", user1Id, user2Id, matchEventId);

            outboxService.saveEvent(
                    kafkaProperties.match(),
                    new MatchEvent(matchEventId, user1Id, user2Id)
            );

            return new SwipeResponseDto(true);
        }

        return new SwipeResponseDto(false);
    }


    private boolean isFirstUser(UUID id1, UUID id2) {
        return id1.toString().compareTo(id2.toString()) < 0;
    }


    private UUID generateDeterministicMatchId(UUID id1, UUID id2) {
        String pairKey = isFirstUser(id1, id2)
                ? id1.toString() + "_" + id2.toString()
                : id2.toString() + "_" + id1.toString();

        return UUID.nameUUIDFromBytes(pairKey.getBytes(StandardCharsets.UTF_8));
    }
}
