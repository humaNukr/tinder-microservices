package com.tinder.chat.chat.dto;

import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String name,
        int age,
        String gender,
        String bio,
        List<String> interests,
        List<String> photos
) {
}