package com.founderlink.startup.exception;

public class StartupNotFoundException
        extends RuntimeException {

    public StartupNotFoundException(String message) {
        super(message);
    }
}