package com.founderlink.auth.exception;

public class UserServiceConflictException extends UserServiceClientException {

    public UserServiceConflictException(String methodKey, String reason) {
        super(methodKey, 409, reason);
    }
}
