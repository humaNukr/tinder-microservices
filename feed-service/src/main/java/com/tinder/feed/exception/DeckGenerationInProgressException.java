package com.tinder.feed.exception;

public class DeckGenerationInProgressException extends RuntimeException {
    public DeckGenerationInProgressException(String message) {
        super(message);
    }
}