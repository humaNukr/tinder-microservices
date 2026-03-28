package com.tinder.swipe.service.interfaces;

import com.tinder.swipe.dto.swipe.SwipeRequestDto;
import com.tinder.swipe.dto.swipe.SwipeResponseDto;

import java.util.UUID;

public interface SwipeSevice {
    SwipeResponseDto processSwipe(UUID actorId, SwipeRequestDto requestDto);
}
