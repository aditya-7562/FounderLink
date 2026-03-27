package com.founderlink.auth.controller;

import com.founderlink.auth.config.RefreshTokenProperties;
import com.founderlink.auth.dto.*;
import com.founderlink.auth.exception.InvalidRefreshTokenException;
import com.founderlink.auth.service.AuthService;
import com.founderlink.auth.service.AuthSession;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and session management")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenProperties refreshTokenProperties;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns registration details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed — invalid request body"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates a user and returns an authentication response.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User logged in successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed — invalid request body"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthSession authSession = authService.login(request);
        addRefreshTokenCookie(response, authSession.refreshToken());
        return ResponseEntity.ok(authSession.authResponse());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh authentication token", description = "Refreshes the authentication token using a valid refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
            @ApiResponse(responseCode = "403", description = "Refresh token has been revoked")
    })
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveRefreshToken(request);
        AuthSession authSession = authService.refresh(refreshToken);
        addRefreshTokenCookie(response, authSession.refreshToken());
        return ResponseEntity.ok(authSession.authResponse());
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Logs out the user and clears the refresh token cookie.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User logged out successfully")
    })
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = resolveRefreshToken(request);
            if (StringUtils.hasText(refreshToken)) {
                authService.logout(refreshToken);
            }
        } catch (Exception ex) {
            log.debug("Logout cleanup performed");
        }

        clearRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    private String resolveRefreshToken(HttpServletRequest request) {
        String cookieToken = extractRefreshTokenFromCookie(request);
        if (StringUtils.hasText(cookieToken)) {
            return cookieToken;
        }

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorizationHeader)) {
            if (authorizationHeader.startsWith("Bearer ")) {
                return authorizationHeader.substring(7).trim();
            }
            return authorizationHeader.trim();
        }

        return null;
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (refreshTokenProperties.getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(refreshTokenProperties.getCookieName(), refreshToken)
                .httpOnly(true)
                .secure(refreshTokenProperties.isCookieSecure())
                .path(refreshTokenProperties.getCookiePath())
                .maxAge(refreshTokenProperties.getExpiration())
                .sameSite(refreshTokenProperties.getCookieSameSite())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(refreshTokenProperties.getCookieName(), "")
                .httpOnly(true)
                .secure(refreshTokenProperties.isCookieSecure())
                .path(refreshTokenProperties.getCookiePath())
                .maxAge(0)
                .sameSite(refreshTokenProperties.getCookieSameSite())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Sends a 6-digit PIN to the user's email for password reset.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "PIN sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid email format"),
        @ApiResponse(responseCode = "401", description = "Email not found")
    })
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with PIN", description = "Resets the user's password using the PIN sent to their email.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid PIN, expired PIN, or validation error"),
        @ApiResponse(responseCode = "401", description = "User not found")
    })
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordResponse response = authService.resetPassword(
                request.getEmail(),
                request.getPin(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(response);
    }
}
