package com.tinder.auth.publisher;

import com.tinder.auth.event.ActivityType;

import java.util.UUID;

public interface UserActivityPublisher {
	void publishActivity(UUID userId, ActivityType type);
}
