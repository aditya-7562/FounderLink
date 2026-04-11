package com.founderlink.wallet.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.founderlink.wallet.dto.response.ApiResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleWalletNotFound() {
        WalletNotFoundException ex = new WalletNotFoundException("Wallet not found");
        ResponseEntity<ApiResponse<Void>> response = handler.handleWalletNotFound(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Wallet not found");
    }

    @Test
    void handleInsufficientBalance() {
        InsufficientBalanceException ex = new InsufficientBalanceException("Insufficient balance");
        ResponseEntity<ApiResponse<Void>> response = handler.handleInsufficientBalance(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Insufficient balance");
    }

    @Test
    void handleValidation() throws NoSuchMethodException {
        var method = GlobalExceptionHandler.class.getMethod("handleValidation", MethodArgumentNotValidException.class);
        org.springframework.core.MethodParameter parameter = new org.springframework.core.MethodParameter(method, 0);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "field", "error message"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("field", "error message");
    }

    @Test
    void handleGeneralException() {
        Exception ex = new Exception("Unexpected error");
        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneralException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }
}
