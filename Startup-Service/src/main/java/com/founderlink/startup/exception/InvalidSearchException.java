package com.founderlink.startup.exception;

public class InvalidSearchException
        extends RuntimeException {

    public InvalidSearchException(String message) {
        super(message);
    }
}