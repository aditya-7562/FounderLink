package com.founderlink.auth.security;

import com.founderlink.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtProperties jwtProperties;
    
    @Mock
    private Clock clock;

    @InjectMocks
    private JwtService jwtService;

    private static final String VALID_SECRET = "VGhpc0lzQVN0cm9uZ0pXVFNlY3JldEtleUZvclRlc3RzMTIzNDU2Nzg5MDEy";
    private static final String SHORT_SECRET = "short-secret";

    private Instant mockCurrentTime;

    @BeforeEach
    void setUp() {
        // use real instant
    }

    @Test
    void initShouldInitializeSuccessfullyWithBase64Secret() {
        when(jwtProperties.getSecret()).thenReturn(VALID_SECRET);
        jwtService.init();
    }

    @Test
    void initShouldInitializeSuccessfullyWithPlaintextSecret() {
        when(jwtProperties.getSecret()).thenReturn("this-is-a-plain-text-secret-that-is-at-least-32-bytes");
        jwtService.init();
    }

    @Test
    void initShouldThrowExceptionWhenSecretIsTooShort() {
        when(jwtProperties.getSecret()).thenReturn(SHORT_SECRET);

        assertThatThrownBy(() -> jwtService.init())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("JWT secret must be at least 256 bits (32 bytes)");
    }

    @Test
    void generateTokenShouldReturnValidJwtToken() {
        when(jwtProperties.getSecret()).thenReturn(VALID_SECRET);
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(Duration.ofMinutes(15));
        when(clock.instant()).thenAnswer(i -> Instant.now());
        
        jwtService.init();
        
        String token = jwtService.generateToken(123L, "FOUNDER");
        
        assertThat(token).isNotBlank();
        
        Long userId = jwtService.extractUserId(token);
        String role = jwtService.extractRole(token);
        
        assertThat(userId).isEqualTo(123L);
        assertThat(role).isEqualTo("FOUNDER");
    }

    @Test
    void validateTokenShouldReturnTrueForValidToken() {
        when(jwtProperties.getSecret()).thenReturn(VALID_SECRET);
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(Duration.ofMinutes(15));
        when(clock.instant()).thenAnswer(i -> Instant.now());
        
        jwtService.init();
        
        String token = jwtService.generateToken(123L, "FOUNDER");
        
        boolean isValid = jwtService.validateToken(token);
        assertThat(isValid).isTrue();
    }

    @Test
    void validateTokenShouldReturnFalseForExpiredToken() {
        when(jwtProperties.getSecret()).thenReturn(VALID_SECRET);
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(Duration.ofMillis(-1000)); // Expired!
        when(clock.instant()).thenAnswer(i -> Instant.now());
        
        jwtService.init();
        
        String token = jwtService.generateToken(123L, "FOUNDER");
        
        boolean isValid = jwtService.validateToken(token);
        assertThat(isValid).isFalse();
    }

    @Test
    void validateTokenShouldReturnFalseForMalformedToken() {
        when(jwtProperties.getSecret()).thenReturn(VALID_SECRET);
        jwtService.init();
        
        boolean isValid = jwtService.validateToken("this.is.not.a.real.jwt.token");
        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenExpiredShouldWorkCorrectly() {
        when(jwtProperties.getSecret()).thenReturn(VALID_SECRET);
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(Duration.ofMillis(-1000));
        when(clock.instant()).thenAnswer(i -> Instant.now());
        
        jwtService.init();
        String token = jwtService.generateToken(123L, "FOUNDER");
        
        assertThatThrownBy(() -> jwtService.isTokenExpired(token))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
}
