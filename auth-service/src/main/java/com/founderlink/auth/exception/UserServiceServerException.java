package com.founderlink.auth.exception;

public class UserServiceServerException extends UserServiceClientException {

    public UserServiceServerException(String methodKey, int status, String reason) {
        super(methodKey, status, reason);
    }
}
