package com.founderlink.gateway.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.founderlink.gateway.security.AuthenticatedUser;
import com.founderlink.gateway.security.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    
    private final JwtParser jwtParser;
    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        log.info("Initializing JwtService with secret length: {}", secret.length());
        log.debug("Secret first 10 chars: {}", secret.substring(0, Math.min(10, secret.length())));
        
        byte[] keyBytes;
        
        // Try to decode as Base64 first (matching auth-service behavior)
        try {
            keyBytes = Base64.getDecoder().decode(secret);
            log.info("Successfully decoded Base64 secret. Raw bytes length: {}", keyBytes.length);
        } catch (IllegalArgumentException e) {
            // Not Base64 encoded, use raw bytes
            log.info("Secret is not Base64 encoded, using raw bytes");
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            log.debug("Raw bytes length: {}", keyBytes.length);
        }
        
        // Validate key length (must be at least 32 bytes for HS256)
        if (keyBytes.length < 32) {
            log.error("JWT secret is too short: {} bytes. Minimum required: 32 bytes", keyBytes.length);
            throw new IllegalArgumentException("JWT secret must be at least 256 bits (32 bytes)");
        }
        
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parser()
                .verifyWith(this.signingKey)
                .build();
        
        log.info("JwtService initialized successfully with key algorithm: {}", signingKey.getAlgorithm());
    }

    public AuthenticatedUser authenticate(String token) {
        log.debug("Starting token authentication");
        
        if (!StringUtils.hasText(token)) {
            log.warn("Authentication failed: Missing bearer token");
            throw unauthorized("Missing bearer token");
        }
        
        log.debug("Token length: {} chars", token.length());
        
        // Decode token payload for debugging (without validation)
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                log.debug("Token payload: {}", payload);
            }
        } catch (Exception e) {
            log.debug("Could not decode token payload: {}", e.getMessage());
        }

        try {
            Claims claims = parseClaims(token);
            
            String userId = claims.getSubject();
            if (!StringUtils.hasText(userId)) {
                log.warn("Token subject is missing");
                throw unauthorized("Token subject is missing");
            }
            
            // Log all claims for debugging
            log.debug("Claims extracted - subject: {}, role: {}, expiration: {}", 
                userId, 
                claims.get("role"), 
                claims.getExpiration());
            
            Role role = extractRole(claims.get("role"));
            
            log.info("Authentication successful - userId: {}, role: {}", userId, role);
            return new AuthenticatedUser(userId, role);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during authentication", e);
            throw unauthorized("Authentication failed: " + e.getMessage());
        }
    }

    private Claims parseClaims(String token) {
        try {
            log.debug("Parsing and validating JWT token");
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            
            // Explicitly check expiration
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                log.warn("Token expired at: {}", expiration);
                throw unauthorized("Token has expired");
            }
            
            log.debug("Token validation successful");
            return claims;
            
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.warn("Token expired: {}", ex.getMessage());
            throw unauthorized("Token has expired");
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            log.error("Invalid token signature - possible secret key mismatch", ex);
            log.error("Check that gateway and auth-service use the same JWT secret");
            throw unauthorized("Invalid token signature");
        } catch (io.jsonwebtoken.MalformedJwtException ex) {
            log.error("Malformed token: {}", ex.getMessage());
            throw unauthorized("Malformed token");
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("JWT validation failed: {}", ex.getMessage());
            throw unauthorized("Invalid or expired token");
        }
    }

    private Role extractRole(Object roleClaim) {
        log.debug("Extracting role from claim: {}", roleClaim);
        
        if (roleClaim == null) {
            log.warn("Token role claim is missing");
            throw unauthorized("Token role is missing");
        }
        
        // Handle collection/array (if multiple roles)
        if (roleClaim instanceof Collection<?>) {
            Collection<?> roles = (Collection<?>) roleClaim;
            if (roles.isEmpty()) {
                throw unauthorized("Token role collection is empty");
            }
            roleClaim = roles.iterator().next();
            log.debug("Extracted first role from collection: {}", roleClaim);
        } else if (roleClaim.getClass().isArray()) {
            Object[] roles = (Object[]) roleClaim;
            if (roles.length == 0) {
                throw unauthorized("Token role array is empty");
            }
            roleClaim = roles[0];
            log.debug("Extracted first role from array: {}", roleClaim);
        }

        String roleValue = roleClaim.toString().trim();
        if (!StringUtils.hasText(roleValue)) {
            throw unauthorized("Token role is empty");
        }
        
        if (roleValue.contains(",")) {
            log.warn("Multiple roles found in token: {}", roleValue);
            throw unauthorized("Token must contain exactly one role");
        }

        try {
            Role role = Role.valueOf(roleValue.toUpperCase(Locale.ROOT));
            log.debug("Role extracted successfully: {}", role);
            return role;
        } catch (IllegalArgumentException ex) {
            log.error("Invalid role value: {}", roleValue);
            throw unauthorized("Token role is invalid: " + roleValue);
        }
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}