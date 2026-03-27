package com.founderlink.auth.exception;

public class ExpiredPasswordResetPinException extends RuntimeException {
    public ExpiredPasswordResetPinException(String message) {
        super(message);
    }
}
