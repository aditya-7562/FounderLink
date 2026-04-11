package com.founderlink.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.founderlink.auth.dto.LoginRequest;
import com.founderlink.auth.entity.Role;
import com.founderlink.auth.entity.User;
import com.founderlink.auth.entity.UserStatus;
import com.founderlink.auth.exception.AccountBannedException;
import com.founderlink.auth.exception.InvalidRefreshTokenException;
import com.founderlink.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceCoverageTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private com.founderlink.auth.security.JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_BannedUser_ThrowsAccountBannedException() {
        LoginRequest request = new LoginRequest("banned@test.com", "password");
        User user = new User();
        user.setEmail("banned@test.com");
        user.setStatus(UserStatus.BANNED);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountBannedException.class)
                .hasMessageContaining("banned");
    }

    @Test
    void refresh_BannedUser_ThrowsAccountBannedException() {
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.SUSPENDED);
        
        var refreshToken = com.founderlink.auth.entity.RefreshToken.builder().userId(1L).build();

        when(refreshTokenService.validateToken(anyString())).thenReturn(refreshToken);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh("some-token"))
                .isInstanceOf(AccountBannedException.class)
                .hasMessageContaining("suspended");
    }

    @Test
    void logout_HandlesExceptionsSilently() {
        doThrow(new InvalidRefreshTokenException("Invalid")).when(refreshTokenService).revokeToken(anyString());
        
        // Should not throw
        authService.logout("invalid-token");
        
        verify(refreshTokenService).revokeToken("invalid-token");
    }
}
