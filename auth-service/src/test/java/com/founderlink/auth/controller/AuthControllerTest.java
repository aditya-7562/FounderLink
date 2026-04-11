package com.founderlink.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.auth.config.RefreshTokenProperties;
import com.founderlink.auth.dto.*;
import com.founderlink.auth.entity.Role;
import com.founderlink.auth.exception.GlobalExceptionHandler;
import com.founderlink.auth.exception.InvalidRefreshTokenException;
import com.founderlink.auth.service.AuthService;
import com.founderlink.auth.service.AuthSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RefreshTokenProperties refreshTokenProperties = new RefreshTokenProperties();
        refreshTokenProperties.setExpiration(Duration.ofDays(30));
        refreshTokenProperties.setCookieName("refresh_token");
        refreshTokenProperties.setCookiePath("/auth");
        refreshTokenProperties.setCookieSameSite("None");
        refreshTokenProperties.setCookieSecure(true);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService, refreshTokenProperties))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerShouldReturnSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("Test User", "test@founderlink.com", "pass123", Role.FOUNDER);
        RegisterResponse response = RegisterResponse.builder()
                .email("test@founderlink.com")
                .role("FOUNDER")
                .message("User registered successfully")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@founderlink.com"))
                .andExpect(jsonPath("$.role").value("FOUNDER"))
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    void loginShouldAuthenticateAndReturnJwtResponse() throws Exception {
        LoginRequest request = new LoginRequest("test@founderlink.com", "pass123");
        AuthResponse authResponse = AuthResponse.builder()
                .token("access-token")
                .email("test@founderlink.com")
                .role("FOUNDER")
                .userId(1L)
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthSession(authResponse, "refresh-token-value"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().value("refresh_token", "refresh-token-value"))
                .andExpect(jsonPath("$.token").value("access-token"));
    }

    @Test
    void refreshShouldReturnNewAccessTokenAndRotateRefreshCookie() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .token("new-access-token")
                .email("alice@founderlink.com")
                .role("FOUNDER")
                .userId(25L)
                .build();

        when(authService.refresh("incoming-refresh-token"))
                .thenReturn(new AuthSession(authResponse, "rotated-refresh-token"));

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "incoming-refresh-token"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(cookie().value("refresh_token", "rotated-refresh-token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().secure("refresh_token", true))
                .andExpect(jsonPath("$.token").value("new-access-token"))
                .andExpect(jsonPath("$.email").value("alice@founderlink.com"))
                .andExpect(jsonPath("$.role").value("FOUNDER"))
                .andExpect(jsonPath("$.userId").value(25L));

        verify(authService).refresh("incoming-refresh-token");
    }

    @Test
    void refreshShouldLocateTokenInAuthorizationHeaderWhenCookieIsMissing() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .token("new-access-token")
                .email("bob@founderlink.com")
                .role("INVESTOR")
                .userId(10L)
                .build();

        when(authService.refresh("header-refresh-token"))
                .thenReturn(new AuthSession(authResponse, "rotated-refresh-token"));

        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer header-refresh-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(cookie().value("refresh_token", "rotated-refresh-token"));

        verify(authService).refresh("header-refresh-token");
    }
    
    @Test
    void refreshShouldLocateTokenInAuthorizationHeaderWithoutBearerPrefix() throws Exception {
        AuthResponse authResponse = AuthResponse.builder().token("token").build();
        when(authService.refresh("raw-header-token")).thenReturn(new AuthSession(authResponse, "new-rt"));

        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "raw-header-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void refreshShouldThrowWhenNoTokenFound() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    void logoutShouldClearCookieAndInvokeServiceWithValidToken() throws Exception {
        doNothing().when(authService).logout("valid-refresh-token");

        mockMvc.perform(post("/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "valid-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().value("refresh_token", ""))
                .andExpect(cookie().maxAge("refresh_token", 0));
                
        verify(authService).logout("valid-refresh-token");
    }

    @Test
    void logoutShouldStillClearCookieAndNotThrowIfTokenIsMissingOrInvalid() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().value("refresh_token", ""))
                .andExpect(cookie().maxAge("refresh_token", 0));
                
        verify(authService, never()).logout(any());
    }

    @Test
    void forgotPasswordShouldReturnSuccess() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("forgot@founderlink.com");
        ForgotPasswordResponse response = new ForgotPasswordResponse("PIN sent successfully");

        when(authService.forgotPassword("forgot@founderlink.com")).thenReturn(response);

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("PIN sent successfully"));
    }

    @Test
    void resetPasswordShouldReturnSuccess() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("reset@founderlink.com");
        request.setPin("123456");
        request.setNewPassword("SecurePass1!");
        ResetPasswordResponse response = new ResetPasswordResponse("Password reset successfully");

        when(authService.resetPassword("reset@founderlink.com", "123456", "SecurePass1!")).thenReturn(response);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }
}
