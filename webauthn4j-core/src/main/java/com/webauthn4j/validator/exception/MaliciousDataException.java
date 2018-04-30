package com.webauthn4j.validator.exception;

public class MaliciousDataException extends ValidationException {
    public MaliciousDataException(String message) {
        super(message);
    }

    public MaliciousDataException(String message, Throwable cause) {
        super(message, cause);
    }
}