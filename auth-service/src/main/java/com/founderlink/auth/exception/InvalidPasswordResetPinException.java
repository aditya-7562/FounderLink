package com.founderlink.auth.exception;

public class InvalidPasswordResetPinException extends RuntimeException {
    public InvalidPasswordResetPinException(String message) {
        super(message);
    }
}
