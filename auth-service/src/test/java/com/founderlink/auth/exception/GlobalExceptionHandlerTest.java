package com.founderlink.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAllShouldReturnGenericMessageForAdminSeedingException() {

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test");

        ResponseEntity<ApiError> response =
                handler.handleAll(new AdminSeedingException("admin seed failed"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);

        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Something went wrong");
        assertThat(body.path()).isEqualTo("/test");
    }

    @Test
    void handleAllShouldReturnGenericMessageForGenericException() {

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/error");

        ResponseEntity<ApiError> response =
                handler.handleAll(new IllegalStateException("unexpected failure"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);

        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Something went wrong");
        assertThat(body.path()).isEqualTo("/error");
    }

    @Test
    void handleEmailExistsShouldReturn409() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/auth/register");

        ResponseEntity<ApiError> response =
                handler.handleEmailExists(new EmailAlreadyExistsException("Email already registered"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Email already registered");
        assertThat(response.getBody().path()).isEqualTo("/auth/register");
    }

    @Test
    void handleBadCredentialsShouldReturn401() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/auth/login");

        ResponseEntity<ApiError> response =
                handler.handleBadCredentials(request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid email or password");
        assertThat(response.getBody().path()).isEqualTo("/auth/login");
    }

    @Test
    void handleAccessDeniedShouldReturn403() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/auth/logout");

        AccessDeniedException ex = new AccessDeniedException("Requested role is not allowed");

        ResponseEntity<ApiError> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Access denied");
        assertThat(response.getBody().path()).isEqualTo("/auth/logout");
    }

    @Test
    void handleAccessDeniedShouldFallbackMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/auth/logout");

        AccessDeniedException ex = new AccessDeniedException(null);

        ResponseEntity<ApiError> response = handler.handleAccessDenied(ex, request);

        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().message()).isEqualTo("Access denied");
    }
}
