package com.founderlink.auth.service;

import com.founderlink.auth.config.RefreshTokenProperties;
import com.founderlink.auth.entity.RefreshToken;
import com.founderlink.auth.exception.ExpiredRefreshTokenException;
import com.founderlink.auth.exception.RevokedRefreshTokenException;
import com.founderlink.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    private RefreshTokenService refreshTokenService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        RefreshTokenProperties properties = new RefreshTokenProperties();
        properties.setExpiration(Duration.ofDays(30));
        properties.setCookieName("refresh_token");
        properties.setCookiePath("/auth");
        properties.setCookieSameSite("None");
        properties.setCookieSecure(true);

        clock = Clock.fixed(Instant.parse("2026-03-18T10:15:30Z"), ZoneOffset.UTC);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, properties, clock);
    }

    @Test
    void validateTokenShouldRejectExpiredToken() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .token(hash("expired-token"))
                .userId(99L)
                .expiryDate(Instant.parse("2026-03-18T10:15:29Z"))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(expiredToken.getToken())).thenReturn(Optional.of(expiredToken));

        ExpiredRefreshTokenException exception = assertThrows(
                ExpiredRefreshTokenException.class,
                () -> refreshTokenService.validateToken("expired-token")
        );

        assertThat(exception.getMessage()).isEqualTo("Refresh token has expired");
    }

    @Test
    void validateTokenShouldRejectRevokedToken() {
        RefreshToken revokedToken = RefreshToken.builder()
                .id(2L)
                .token(hash("revoked-token"))
                .userId(100L)
                .expiryDate(Instant.parse("2026-03-19T10:15:30Z"))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByToken(revokedToken.getToken())).thenReturn(Optional.of(revokedToken));

        RevokedRefreshTokenException exception = assertThrows(
                RevokedRefreshTokenException.class,
                () -> refreshTokenService.validateToken("revoked-token")
        );

        assertThat(exception.getMessage()).isEqualTo("Refresh token has been revoked");
    }

    @Test
    void rotateTokenShouldRevokeCurrentTokenAndCreateReplacement() {
        RefreshToken currentToken = RefreshToken.builder()
                .id(3L)
                .token(hash("active-token"))
                .userId(55L)
                .expiryDate(Instant.parse("2026-03-19T10:15:30Z"))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenForUpdate(currentToken.getToken())).thenReturn(Optional.of(currentToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String rotatedToken = refreshTokenService.rotateToken("active-token");

        assertThat(rotatedToken).isNotBlank();
        assertThat(rotatedToken).isNotEqualTo("active-token");

        verify(refreshTokenRepository, times(2)).save(refreshTokenCaptor.capture());
        List<RefreshToken> savedTokens = refreshTokenCaptor.getAllValues();

        RefreshToken revokedToken = savedTokens.get(0);
        assertThat(revokedToken.isRevoked()).isTrue();
        assertThat(revokedToken.getRevokedAt()).isEqualTo(Instant.now(clock));
        assertThat(revokedToken.getUserId()).isEqualTo(55L);

        RefreshToken replacementToken = savedTokens.get(1);
        assertThat(replacementToken.isRevoked()).isFalse();
        assertThat(replacementToken.getUserId()).isEqualTo(55L);
        assertThat(replacementToken.getExpiryDate()).isEqualTo(Instant.now(clock).plus(Duration.ofDays(30)));
        assertThat(replacementToken.getToken()).hasSize(64);
        assertThat(replacementToken.getToken()).isNotEqualTo(currentToken.getToken());
    }


    @Test
    void createTokenShouldEvictOldestTokenWhenMaxSessionsReached() {
        when(refreshTokenRepository.countByUserIdAndRevokedFalse(99L)).thenReturn(5L);
        
        RefreshToken oldestToken = RefreshToken.builder()
                .id(10L)
                .userId(99L)
                .token(hash("oldest-token-hash"))
                .revoked(false)
                .build();
        
        when(refreshTokenRepository.findOldestActiveToken(99L)).thenReturn(Optional.of(oldestToken));
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        refreshTokenService.createToken(99L);
        
        verify(refreshTokenRepository, times(2)).save(refreshTokenCaptor.capture());
        
        RefreshToken revokedToken = refreshTokenCaptor.getAllValues().get(0);
        assertThat(revokedToken.isRevoked()).isTrue();
        assertThat(revokedToken.getId()).isEqualTo(10L);
    }

    @Test
    void validateTokenShouldThrowWhenTokenNotFound() {
        when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.empty());
        assertThrows(com.founderlink.auth.exception.InvalidRefreshTokenException.class, () -> refreshTokenService.validateToken("non-existent"));
    }

    @Test
    void validateTokenShouldThrowWhenTokenIsBlank() {
        assertThrows(com.founderlink.auth.exception.InvalidRefreshTokenException.class, () -> refreshTokenService.validateToken("   "));
        assertThrows(com.founderlink.auth.exception.InvalidRefreshTokenException.class, () -> refreshTokenService.validateToken(null));
    }

    @Test
    void revokeTokenShouldRevokeSuccessfully() {
        RefreshToken validToken = RefreshToken.builder()
                .id(5L)
                .token(hash("valid-token"))
                .userId(99L)
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenForUpdate(any())).thenReturn(Optional.of(validToken));
        
        refreshTokenService.revokeToken("valid-token");
        
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue().isRevoked()).isTrue();
    }

    @Test
    void revokeTokenShouldThrowWhenAlreadyRevoked() {
        RefreshToken revokedToken = RefreshToken.builder()
                .id(5L)
                .token(hash("valid-token"))
                .userId(99L)
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByTokenForUpdate(any())).thenReturn(Optional.of(revokedToken));
        
        assertThrows(RevokedRefreshTokenException.class, () -> refreshTokenService.revokeToken("valid-token"));
    }

    @Test
    void rotateTokenShouldThrowWhenTokenNotFoundForUpdate() {
        when(refreshTokenRepository.findByTokenForUpdate(any())).thenReturn(Optional.empty());
        assertThrows(com.founderlink.auth.exception.InvalidRefreshTokenException.class, () -> refreshTokenService.rotateToken("non-existent"));
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
