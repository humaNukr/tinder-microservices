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
            boolean isPremium = false;
            rateLimiterService.checkAndIncrementLikeLimit(actorId, isPremium);
        }

        SwipeStatusProjection projection = actorId.compareTo(targetId) < 0
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

            log.info("Match found! ActorId: {}, TargetId: {}. MatchEventId: {}", actorId, targetId, matchEventId);
            outboxService.saveEvent(
                    kafkaProperties.match(),
                    new MatchEvent(matchEventId, actorId, targetId)
            );

            return new SwipeResponseDto(true);
        }

        return new SwipeResponseDto(false);
    }

    private UUID generateDeterministicMatchId(UUID id1, UUID id2) {
        String pairKey = id1.compareTo(id2) < 0
                ? id1.toString() + id2.toString()
                : id2.toString() + id1.toString();

        return UUID.nameUUIDFromBytes(pairKey.getBytes(StandardCharsets.UTF_8));
    }
}
