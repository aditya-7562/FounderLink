package com.founderlink.team.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Invitation Not Found → 404
    @ExceptionHandler(InvitationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInvitationNotFoundException(InvitationNotFoundException ex) {
        log.error("Invitation not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now()));
    }

    // Team Member Not Found → 404
    @ExceptionHandler(TeamMemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTeamMemberNotFoundException(TeamMemberNotFoundException ex) {
        log.error("Team member not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now()));
    }

    // Startup Not Found → 404
    @ExceptionHandler(StartupNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStartupNotFoundException(StartupNotFoundException ex) {
        log.error("Startup not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now()));
    }

    // Duplicate Invitation → 409
    @ExceptionHandler(DuplicateInvitationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateInvitationException(DuplicateInvitationException ex) {
        log.error("Duplicate invitation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage(), LocalDateTime.now()));
    }

    // Already Team Member → 409
    @ExceptionHandler(AlreadyTeamMemberException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyTeamMemberException(AlreadyTeamMemberException ex) {
        log.error("Already team member: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage(), LocalDateTime.now()));
    }

    // Invalid Invitation Status → 400
    @ExceptionHandler(InvalidInvitationStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInvitationStatusException(InvalidInvitationStatusException ex) {
        log.error("Invalid invitation status: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), LocalDateTime.now()));
    }

    // Unauthorized Access → 403
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
        log.error("Unauthorized access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage(), LocalDateTime.now()));
    }

    // Forbidden Access → 403
    @ExceptionHandler(ForbiddenAccessException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenAccessException(ForbiddenAccessException ex) {
        log.error("Forbidden access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage(), LocalDateTime.now()));
    }

    // Startup Service Unavailable → 503
    // Thrown when circuit is open or retries exhausted — retryable infrastructure failure
    @ExceptionHandler(StartupServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleStartupServiceUnavailableException(StartupServiceUnavailableException ex) {
        log.error("Startup service unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "Startup service is temporarily unavailable. Please try again later.", LocalDateTime.now()));
    }

    // Startup Service Server Error → 502
    // Thrown when Startup-Service returns 5xx — retryable
    @ExceptionHandler(StartupServiceServerException.class)
    public ResponseEntity<ErrorResponse> handleStartupServiceServerException(StartupServiceServerException ex) {
        log.error("Startup service server error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(HttpStatus.BAD_GATEWAY.value(),
                        "Startup service encountered an error. Please try again later.", LocalDateTime.now()));
    }

    // Validation Errors → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation failed: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            errors.put(fieldName, error.getDefaultMessage());
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // Invalid Enum Value → 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error("Invalid request body: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                        "Invalid value provided. Accepted roles are: CTO, CPO, MARKETING_HEAD, ENGINEERING_LEAD",
                        LocalDateTime.now()));
    }

    // Any Other Exception → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Something went wrong. Please try again later.", LocalDateTime.now()));
    }
}
