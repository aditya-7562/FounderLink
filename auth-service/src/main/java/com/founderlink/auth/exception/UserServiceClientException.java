package com.founderlink.auth.exception;

public class UserServiceClientException extends RuntimeException {

    private final String methodKey;
    private final int status;

    public UserServiceClientException(String methodKey, int status, String reason) {
        super("User-service call failed for method=%s with status=%d reason=%s".formatted(methodKey, status, reason));
        this.methodKey = methodKey;
        this.status = status;
    }

    public String getMethodKey() {
        return methodKey;
    }

    public int getStatus() {
        return status;
    }
}
