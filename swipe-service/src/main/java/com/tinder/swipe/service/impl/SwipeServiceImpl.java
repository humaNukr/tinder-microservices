package com.tinder.swipe.service.impl;

import com.tinder.swipe.dto.swipe.SwipeRequestDto;
import com.tinder.swipe.dto.swipe.SwipeResponseDto;
import com.tinder.swipe.dto.swipe.SwipeStatusProjection;
import com.tinder.swipe.repository.SwipeRepository;
import com.tinder.swipe.service.interfaces.SwipeSevice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SwipeServiceImpl implements SwipeSevice {
    private final SwipeRepository swipeRepository;

    public SwipeResponseDto processSwipe(UUID actorId, SwipeRequestDto requestDto) {
        UUID targetId = requestDto.targetId();
        boolean liked = requestDto.isLiked();

        if (actorId.equals(targetId)) {
            throw new IllegalArgumentException("Target Id and actor Id can't be same");
        }

        SwipeStatusProjection projection = actorId.compareTo(targetId) < 0
                ? swipeRepository.upsertSwipeByUser1(actorId, targetId, liked)
                : swipeRepository.upsertSwipeByUser2(targetId, actorId, liked);

        if (Boolean.TRUE.equals(projection.getIsLikedByUser1()) && Boolean.TRUE.equals(projection.getIsLikedByUser2())
        ) {
            log.info("Match found between {} and {}", actorId, targetId);
            //TODO: SEND MatchEvent to KAFKA
            return new SwipeResponseDto(true);
        }

        return new SwipeResponseDto(false);
    }
}
