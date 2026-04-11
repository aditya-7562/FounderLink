package com.founderlink.payment.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlePaymentNotFound() {
        PaymentNotFoundException ex = new PaymentNotFoundException("Not found");
        ResponseEntity<ErrorResponse> response = handler.handlePaymentNotFound(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Not found");
    }

    @Test
    void handlePaymentGateway() {
        PaymentGatewayException ex = new PaymentGatewayException("Gateway error");
        ResponseEntity<ErrorResponse> response = handler.handlePaymentGateway(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().getMessage()).isEqualTo("Gateway error");
    }

    @Test
    void handleInvalidSignature() {
        InvalidSignatureException ex = new InvalidSignatureException("Invalid signature");
        ResponseEntity<ErrorResponse> response = handler.handleInvalidSignature(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid signature");
    }

    @Test
    void handleInsufficientFunds() {
        InsufficientFundsException ex = new InsufficientFundsException("Insufficient funds");
        ResponseEntity<ErrorResponse> response = handler.handleInsufficientFunds(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Insufficient funds");
    }

    @Test
    void handleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
    }

    @Test
    void handleIdempotency() {
        IdempotencyException ex = new IdempotencyException("Conflict");
        ResponseEntity<ErrorResponse> response = handler.handleIdempotency(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Conflict");
    }

    @Test
    void handleValidation() throws NoSuchMethodException {
        // Use reflection to get a real MethodParameter to avoid NPE in Spring's Exception logic
        var method = GlobalExceptionHandler.class.getMethod("handleValidation", MethodArgumentNotValidException.class);
        org.springframework.core.MethodParameter parameter = new org.springframework.core.MethodParameter(method, 0);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "field", "must not be empty"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);
        
        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("field", "must not be empty");
    }

    @Test
    void handleUnreadable() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Unreadable");
        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid request body");
    }

    @Test
    void handleIllegalState() {
        IllegalStateException ex = new IllegalStateException("Illegal state");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalState(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Illegal state");
    }

    @Test
    void handleGlobal() {
        Exception ex = new Exception("Unexpected");
        ResponseEntity<ErrorResponse> response = handler.handleGlobal(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Something went wrong. Please try again later.");
    }
}
