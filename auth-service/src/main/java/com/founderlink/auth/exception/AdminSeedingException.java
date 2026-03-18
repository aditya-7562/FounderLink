package com.founderlink.auth.exception;

public class AdminSeedingException extends RuntimeException {

    public AdminSeedingException(String message) {
        super(message);
    }

    public AdminSeedingException(String message, Throwable cause) {
        super(message, cause);
    }
}
