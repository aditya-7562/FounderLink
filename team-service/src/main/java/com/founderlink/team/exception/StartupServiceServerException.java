package com.founderlink.team.exception;

public class StartupServiceServerException extends RuntimeException {

    private final String methodKey;
    private final int status;

    public StartupServiceServerException(String methodKey, int status, String reason) {
        super("Startup service error [" + status + "] on " + methodKey + ": " + reason);
        this.methodKey = methodKey;
        this.status = status;
    }

    public String getMethodKey() { return methodKey; }
    public int getStatus()       { return status; }
}
