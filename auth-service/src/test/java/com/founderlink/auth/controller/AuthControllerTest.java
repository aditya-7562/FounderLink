package com.founderlink.auth.controller;

import com.founderlink.auth.config.RefreshTokenProperties;
import com.founderlink.auth.dto.AuthResponse;
import com.founderlink.auth.exception.GlobalExceptionHandler;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    @BeforeEach
    void setUp() {
        RefreshTokenProperties refreshTokenProperties = new RefreshTokenProperties();
        refreshTokenProperties.setExpiration(Duration.ofDays(30));
        refreshTokenProperties.setCookieName("refresh_token");
        refreshTokenProperties.setCookiePath("/auth");
        refreshTokenProperties.setCookieSameSite("Strict");
        refreshTokenProperties.setCookieSecure(true);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService, refreshTokenProperties))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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
}
