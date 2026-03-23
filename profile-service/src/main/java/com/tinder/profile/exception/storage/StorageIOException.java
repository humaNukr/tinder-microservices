package com.tinder.profile.exception.storage;

public class StorageIOException extends RuntimeException {
    public StorageIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
