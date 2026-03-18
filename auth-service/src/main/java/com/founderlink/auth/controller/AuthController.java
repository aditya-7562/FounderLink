package com.founderlink.auth.controller;

import com.founderlink.auth.config.RefreshTokenProperties;
import com.founderlink.auth.dto.AuthResponse;
import com.founderlink.auth.dto.LoginRequest;
import com.founderlink.auth.dto.RegisterRequest;
import com.founderlink.auth.dto.RegisterResponse;
import com.founderlink.auth.exception.InvalidRefreshTokenException;
import com.founderlink.auth.service.AuthService;
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

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenProperties refreshTokenProperties;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        var authSession = authService.login(request);
        addRefreshTokenCookie(response, authSession.refreshToken());
        return ResponseEntity.ok(authSession.authResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveRefreshToken(request);
        var authSession = authService.refresh(refreshToken);
        addRefreshTokenCookie(response, authSession.refreshToken());
        return ResponseEntity.ok(authSession.authResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = resolveRefreshToken(request);
            authService.logout(refreshToken);
        } catch (InvalidRefreshTokenException ex) {
            log.debug("Logout request completed without a valid refresh token");
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

        throw new InvalidRefreshTokenException("Refresh token is missing");
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
}
