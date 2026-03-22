package com.founderlink.gateway.service;

import com.founderlink.gateway.security.AuthenticatedUser;
import com.founderlink.gateway.security.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;

@Service
public class JwtService {

    private final JwtParser jwtParser;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.jwtParser = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    public AuthenticatedUser authenticate(String token) {
        if (!StringUtils.hasText(token)) {
            throw unauthorized("Missing bearer token");
        }

        Claims claims = parseClaims(token);
        String userId = claims.getSubject();
        if (!StringUtils.hasText(userId)) {
            throw unauthorized("Token subject is missing");
        }

        return new AuthenticatedUser(userId, extractRole(claims.get("role")));
    }

    private Claims parseClaims(String token) {
        try {
            return jwtParser.parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw unauthorized("Invalid or expired token");
        }
    }

    private Role extractRole(Object roleClaim) {
        if (roleClaim == null) {
            throw unauthorized("Token role is missing");
        }
        if (roleClaim instanceof Collection<?> || roleClaim.getClass().isArray()) {
            throw unauthorized("Token must contain exactly one role");
        }

        String roleValue = roleClaim.toString().trim();
        if (!StringUtils.hasText(roleValue) || roleValue.contains(",")) {
            throw unauthorized("Token must contain exactly one role");
        }

        try {
            return Role.valueOf(roleValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw unauthorized("Token role is invalid");
        }
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
