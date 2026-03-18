package com.founderlink.auth.exception;

public class UserServiceBadRequestException extends UserServiceClientException {

    public UserServiceBadRequestException(String methodKey, String reason) {
        super(methodKey, 400, reason);
    }
}
