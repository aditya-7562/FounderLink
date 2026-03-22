package com.founderlink.messaging.exception;

public class MessageNotFoundException extends RuntimeException {

    public MessageNotFoundException(String message) {
        super(message);
    }

    public MessageNotFoundException(Long id) {
        super("Message not found with id: " + id);
    }
}
