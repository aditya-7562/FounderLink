package com.founderlink.payment.exception;

public class IdempotencyException extends RuntimeException {

    public IdempotencyException(String message) {
        super(message);
    }
}
