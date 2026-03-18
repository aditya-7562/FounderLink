package com.founderlink.auth.exception;

public class UserServiceUnavailableException extends UserServiceClientException {

    public UserServiceUnavailableException(String methodKey, String reason) {
        super(methodKey, 503, reason);
    }
}
