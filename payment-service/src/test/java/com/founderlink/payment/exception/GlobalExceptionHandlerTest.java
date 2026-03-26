package com.founderlink.payment.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handlePaymentNotFoundException() {
        PaymentNotFoundException ex = new PaymentNotFoundException("Not found");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handlePaymentNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Not found", response.getBody().getMessage());
    }

    @Test
    void handlePaymentGatewayException() {
        PaymentGatewayException ex = new PaymentGatewayException("Gateway issue");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handlePaymentGateway(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Gateway issue", response.getBody().getMessage());
    }

    @Test
    void handleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access Denied");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Access Denied", response.getBody().getMessage());
    }
}
