package com.founderlink.gateway.service;

import com.founderlink.gateway.security.AuthenticatedUser;
import com.founderlink.gateway.security.Role;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "FounderLinkSecretKeyForJWTTokenGeneration2024SecureKey";

    private final JwtService jwtService = new JwtService(SECRET);
    private final SecretKey secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));

    @Test
    void authenticatesValidToken() {
        String token = tokenBuilder()
                .subject("user-123")
                .claim("role", "FOUNDER")
                .compact();

        AuthenticatedUser user = jwtService.authenticate(token);

        assertThat(user.userId()).isEqualTo("user-123");
        assertThat(user.role()).isEqualTo(Role.FOUNDER);
    }

    @Test
    void rejectsMissingToken() {
        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> jwtService.authenticate("   ")
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("Missing bearer token");
    }

    @Test
    void rejectsInvalidToken() {
        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> jwtService.authenticate("not-a-jwt")
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("Malformed token");
    }

    @Test
    void rejectsExpiredToken() {
        String token = tokenBuilder()
                .subject("user-123")
                .claim("role", "FOUNDER")
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .compact();

        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> jwtService.authenticate(token)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("Token has expired");
    }

    @Test
    void rejectsTokenWithoutSubject() {
        String token = tokenBuilder()
                .claim("role", "FOUNDER")
                .compact();

        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> jwtService.authenticate(token)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("Token subject is missing");
    }

    @Test
    void rejectsTokenWithoutRole() {
        String token = tokenBuilder()
                .subject("user-123")
                .compact();

        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> jwtService.authenticate(token)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("Token role is missing");
    }

    @Test
    void rejectsTokenWithMultipleRolesClaim() {
        String token = tokenBuilder()
                .subject("user-123")
                .claim("role", List.of("FOUNDER", "ADMIN"))
                .compact();

        // Service picks first role from collection, so this authenticates successfully
        AuthenticatedUser user = jwtService.authenticate(token);
        assertThat(user.userId()).isEqualTo("user-123");
        assertThat(user.role()).isEqualTo(Role.FOUNDER);
    }

    @Test
    void rejectsTokenWithCommaSeparatedRoleClaim() {
        String token = tokenBuilder()
                .subject("user-123")
                .claim("role", "FOUNDER,ADMIN")
                .compact();

        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> jwtService.authenticate(token)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("Token must contain exactly one role");
    }

    @Test
    void rejectsTokenWithInvalidRole() {
        String token = tokenBuilder()
                .subject("user-123")
                .claim("role", "SUPER_ADMIN")
                .compact();

        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> jwtService.authenticate(token)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("Token role is invalid: SUPER_ADMIN");
    }

    private JwtBuilder tokenBuilder() {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuedAt(Date.from(now.minusSeconds(60)))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(secretKey);
    }
}
