package com.founderlink.auth.exception;

public class UserServiceNotFoundException extends UserServiceClientException {

    public UserServiceNotFoundException(String methodKey, String reason) {
        super(methodKey, 404, reason);
    }
}
