package com.founderlink.messaging.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleMessageNotFound - returns 404")
    void handleMessageNotFound_Returns404() {
        MessageNotFoundException ex = new MessageNotFoundException(1L);
        ResponseEntity<ErrorResponse> response = handler.handleMessageNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).contains("1");
    }

    @Test
    @DisplayName("handleInvalidMessage - returns 400")
    void handleInvalidMessage_Returns400() {
        InvalidMessageException ex = new InvalidMessageException("Invalid");
        ResponseEntity<ErrorResponse> response = handler.handleInvalidMessage(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid");
    }

    @Test
    @DisplayName("handleUserServiceUnavailable - returns 503")
    void handleUserServiceUnavailable_Returns503() {
        UserServiceUnavailableException ex = new UserServiceUnavailableException("Down");
        ResponseEntity<ErrorResponse> response = handler.handleUserServiceUnavailable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getMessage()).isEqualTo("Down");
    }

    @Test
    @DisplayName("handleGenericException - returns 500")
    void handleGenericException_Returns500() {
        RuntimeException ex = new RuntimeException("Unexpected");
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred: Unexpected");
    }
}
