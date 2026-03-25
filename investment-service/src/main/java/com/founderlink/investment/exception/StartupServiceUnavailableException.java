package com.founderlink.investment.exception;

public class StartupServiceUnavailableException extends RuntimeException {

    private final String methodKey;

    public StartupServiceUnavailableException(String methodKey, String reason) {
        super("Startup service unavailable on " + methodKey + ": " + reason);
        this.methodKey = methodKey;
    }

    public String getMethodKey() { return methodKey; }
}
