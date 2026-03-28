package com.tinder.swipe.controller;

import com.tinder.swipe.dto.swipe.SwipeRequestDto;
import com.tinder.swipe.dto.swipe.SwipeResponseDto;
import com.tinder.swipe.service.interfaces.SwipeSevice;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/swipes")
public class SwipeController {
    private final SwipeSevice swipeSevice;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public SwipeResponseDto processSwipe(
            @RequestHeader("X-User-Id") UUID id,
            @RequestBody @Valid SwipeRequestDto swipeRequestDto
    ) {
        return swipeSevice.processSwipe(id, swipeRequestDto);
    }
}
