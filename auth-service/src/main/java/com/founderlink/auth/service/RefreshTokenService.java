package com.founderlink.auth.service;

import com.founderlink.auth.config.RefreshTokenProperties;
import com.founderlink.auth.entity.RefreshToken;
import com.founderlink.auth.exception.ExpiredRefreshTokenException;
import com.founderlink.auth.exception.InvalidRefreshTokenException;
import com.founderlink.auth.exception.RevokedRefreshTokenException;
import com.founderlink.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties refreshTokenProperties;
    private final Clock clock;

    @Transactional
    public String createToken(Long userId) {
        String rawToken = generateSecureToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(hashToken(rawToken))
                .userId(userId)
                .expiryDate(Instant.now(clock).plus(refreshTokenProperties.getExpiration()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        log.info("Issued refresh token for userId={}", userId);

        return rawToken;
    }

    @Transactional(readOnly = true)
    public RefreshToken validateToken(String token) {
        RefreshToken refreshToken = findByRawToken(token);
        assertTokenUsable(refreshToken);
        return refreshToken;
    }

    @Transactional
    public void revokeToken(String token) {
        RefreshToken refreshToken = findByRawTokenForUpdate(token);

        if (refreshToken.isRevoked()) {
            log.warn("Refresh token revoke attempt rejected because token is already revoked userId={}", refreshToken.getUserId());
            throw new RevokedRefreshTokenException("Refresh token has been revoked");
        }

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(Instant.now(clock));
        refreshTokenRepository.save(refreshToken);

        log.info("Revoked refresh token for userId={}", refreshToken.getUserId());
    }

    @Transactional
    public String rotateToken(String oldToken) {
        RefreshToken currentToken = findByRawTokenForUpdate(oldToken);
        assertTokenUsable(currentToken);

        currentToken.setRevoked(true);
        currentToken.setRevokedAt(Instant.now(clock));
        refreshTokenRepository.save(currentToken);

        log.info("Rotating refresh token for userId={}", currentToken.getUserId());
        return createToken(currentToken.getUserId());
    }

    private RefreshToken findByRawToken(String rawToken) {
        String tokenHash = hashToken(requireToken(rawToken));
        return refreshTokenRepository.findByToken(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Refresh token validation failed because token was not found");
                    return new InvalidRefreshTokenException("Refresh token is invalid");
                });
    }

    private RefreshToken findByRawTokenForUpdate(String rawToken) {
        String tokenHash = hashToken(requireToken(rawToken));
        return refreshTokenRepository.findByTokenForUpdate(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Refresh token validation failed because token was not found");
                    return new InvalidRefreshTokenException("Refresh token is invalid");
                });
    }

    private void assertTokenUsable(RefreshToken refreshToken) {
        if (refreshToken.isRevoked()) {
            log.warn("Refresh token usage rejected because token is revoked userId={}", refreshToken.getUserId());
            throw new RevokedRefreshTokenException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now(clock))) {
            log.warn("Refresh token usage rejected because token is expired userId={}", refreshToken.getUserId());
            throw new ExpiredRefreshTokenException("Refresh token has expired");
        }
    }

    private String requireToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            log.warn("Refresh token validation failed because token was missing");
            throw new InvalidRefreshTokenException("Refresh token is missing");
        }
        return rawToken.trim();
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
