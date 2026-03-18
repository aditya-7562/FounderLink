package com.founderlink.auth.exception;

public class RevokedRefreshTokenException extends RuntimeException {

    public RevokedRefreshTokenException(String message) {
        super(message);
    }
}
