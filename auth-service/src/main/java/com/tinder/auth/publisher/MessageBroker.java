package com.tinder.auth.publisher;

import com.tinder.auth.entity.OutboxEvent;

import java.util.concurrent.CompletableFuture;

public interface MessageBroker {
    CompletableFuture<OutboxEvent> send(OutboxEvent event);
}
