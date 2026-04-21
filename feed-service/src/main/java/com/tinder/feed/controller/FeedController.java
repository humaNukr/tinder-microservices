package com.tinder.feed.controller;

import com.tinder.feed.dto.ProfileResponse;
import com.tinder.feed.service.interfaces.DeckProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final DeckProvider deckProvider;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProfileResponse> getFeedForUser(@RequestHeader("X-User-Id") UUID userId) {
        return deckProvider.getFeedForUser(userId);
    }
}
