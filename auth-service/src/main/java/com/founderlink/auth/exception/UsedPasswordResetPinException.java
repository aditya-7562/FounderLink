package com.founderlink.auth.exception;

public class UsedPasswordResetPinException extends RuntimeException {
    public UsedPasswordResetPinException(String message) {
        super(message);
    }
}
