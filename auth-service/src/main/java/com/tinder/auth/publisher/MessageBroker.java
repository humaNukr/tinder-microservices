package com.tinder.auth.publisher;

import java.util.concurrent.CompletableFuture;

public interface MessageBroker {
	CompletableFuture<Boolean> send(String topic, String key, String payload);
}
