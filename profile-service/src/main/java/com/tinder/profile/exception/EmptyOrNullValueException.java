package com.tinder.profile.exception;

public class EmptyOrNullValueException extends RuntimeException {
    public EmptyOrNullValueException(String message) {
        super(message);
    }
}