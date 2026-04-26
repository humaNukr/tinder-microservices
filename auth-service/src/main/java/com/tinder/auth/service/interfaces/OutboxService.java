package com.tinder.auth.service.interfaces;

public interface OutboxService {
	void saveEvent(String topic, Object event);
}
