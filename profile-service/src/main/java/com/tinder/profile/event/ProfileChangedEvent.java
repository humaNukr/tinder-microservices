package com.tinder.profile.event;

import com.tinder.profile.dto.ProfileResponse;

public record ProfileChangedEvent(
        ProfileResponse response
) {
}