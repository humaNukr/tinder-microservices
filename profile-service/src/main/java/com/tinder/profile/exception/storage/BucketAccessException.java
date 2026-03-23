package com.tinder.profile.exception.storage;

public class BucketAccessException extends RuntimeException {
    public BucketAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
