package com.tinder.feed.adapter;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;
import java.util.UUID;

public interface ProfileClient {

    @GetExchange("${ProfileServiceProperties}")
    List<UUID> fetchCandidates(@RequestParam("userId") UUID userId);
}