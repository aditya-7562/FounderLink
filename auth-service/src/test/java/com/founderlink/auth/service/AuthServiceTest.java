package com.founderlink.auth.service;

import com.founderlink.auth.dto.AuthResponse;
import com.founderlink.auth.dto.LoginRequest;
import com.founderlink.auth.dto.RegisterRequest;
import com.founderlink.auth.dto.RegisterResponse;
import com.founderlink.auth.entity.Role;
import com.founderlink.auth.entity.RefreshToken;
import com.founderlink.auth.entity.User;
import com.founderlink.auth.exception.EmailAlreadyExistsException;
import com.founderlink.auth.exception.InvalidRefreshTokenException;
import com.founderlink.auth.exception.UserServiceUnavailableException;
import com.founderlink.auth.repository.UserRepository;
import com.founderlink.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private SyncService syncService;

    @Mock
    private com.founderlink.auth.repository.PasswordResetPinRepository passwordResetPinRepository;

    @Mock
    private com.founderlink.auth.publisher.PasswordResetEventPublisher passwordResetEventPublisher;

    @Mock
    private com.founderlink.auth.publisher.UserRegisteredEventPublisher userRegisteredEventPublisher;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<com.founderlink.auth.entity.PasswordResetPin> passwordResetPinCaptor;

    @Captor
    private ArgumentCaptor<com.founderlink.auth.dto.PasswordResetEmailEvent> passwordResetEventCaptor;

    @Test
    void registerShouldPersistUserAndSyncSuccessfully() {
        RegisterRequest request = new RegisterRequest("Alice Founder", "alice@founderlink.com", "PlainPass1", Role.FOUNDER);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(101L);
            return user;
        });
        doNothing().when(syncService).syncUser(any(User.class));

        RegisterResponse response = authService.register(request);

        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getRole()).isEqualTo(Role.FOUNDER.name());
        assertThat(response.getMessage()).isEqualTo("User registered successfully");

        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getName()).isEqualTo(request.getName());
        assertThat(savedUser.getEmail()).isEqualTo(request.getEmail());
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(Role.FOUNDER);

        verify(syncService, times(1)).syncUser(savedUser);
        verify(userRegisteredEventPublisher, times(1)).publishUserRegisteredEvent(any());
    }

    @Test
    void registerShouldFailWhenUserServiceSyncFails() {
        RegisterRequest request = new RegisterRequest("Bob Investor", "bob@founderlink.com", "PlainPass1", Role.INVESTOR);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(202L);
            return user;
        });
        doThrow(new UserServiceUnavailableException("UserClient#createUser", "Service unavailable"))
                .when(syncService).syncUser(any(User.class));

        UserServiceUnavailableException exception = assertThrows(
                UserServiceUnavailableException.class,
                () -> authService.register(request)
        );

        assertThat(exception.getStatus()).isEqualTo(503);
        verify(userRepository).saveAndFlush(any(User.class));
        verify(syncService).syncUser(any(User.class));
    }

    @Test
    void registerShouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Alice Founder", "alice@founderlink.com", "PlainPass1", Role.FOUNDER);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Email already registered");
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(passwordEncoder, syncService);
    }

    @Test
    void registerShouldThrowIllegalArgumentExceptionForMissingRole() {
        RegisterRequest request = new RegisterRequest("Alice Founder", "alice@founderlink.com", "PlainPass1", null);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Role is required");
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(passwordEncoder, syncService);
    }

    @Test
    void registerShouldBlockAdminRoleSelection() {
        RegisterRequest request = new RegisterRequest("Alice Founder", "alice@founderlink.com", "PlainPass1", Role.ADMIN);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> authService.register(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Requested role is not allowed");
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(passwordEncoder, syncService);
    }

    @Test
    void loginShouldAuthenticateAndReturnJwtResponse() {
        LoginRequest request = new LoginRequest("alice@founderlink.com", "PlainPass1");
        User user = new User();
        user.setId(55L);
        user.setEmail(request.getEmail());
        user.setRole(Role.COFOUNDER);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateToken( user.getId(),user.getRole().name())).thenReturn("jwt-token");
        when(refreshTokenService.createToken(user.getId())).thenReturn("refresh-token");

        AuthSession session = authService.login(request);
        AuthResponse response = session.authResponse();

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo(user.getEmail());
        assertThat(response.getRole()).isEqualTo(Role.COFOUNDER.name());
        assertThat(response.getUserId()).isEqualTo(user.getId());
        assertThat(session.refreshToken()).isEqualTo("refresh-token");

        verify(authenticationManager).authenticate(argThat(token ->
                token instanceof UsernamePasswordAuthenticationToken
                        && request.getEmail().equals(token.getPrincipal())
                        && request.getPassword().equals(token.getCredentials())
        ));
        verify(jwtService).generateToken( user.getId(),user.getRole().name());
        verify(refreshTokenService).createToken(user.getId());
    }

    @Test
    void loginShouldPropagateAuthenticationFailure() {
        LoginRequest request = new LoginRequest("alice@founderlink.com", "wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Bad credentials");
        verify(userRepository, never()).findByEmail(any(String.class));
        verify(jwtService, never()).generateToken(any(Long.class), any(String.class));
        verify(refreshTokenService, never()).createToken(any(Long.class));
    }

    @Test
    void refreshShouldReturnNewAccessTokenAndRotatedRefreshToken() {
        String rawRefreshToken = "incoming-refresh-token";
        RefreshToken persistedRefreshToken = RefreshToken.builder()
                .id(11L)
                .token("hashed-token")
                .userId(77L)
                .revoked(false)
                .build();
        User user = new User();
        user.setId(77L);
        user.setEmail("refresh@founderlink.com");
        user.setRole(Role.FOUNDER);

        when(refreshTokenService.validateToken(rawRefreshToken)).thenReturn(persistedRefreshToken);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtService.generateToken( user.getId(),user.getRole().name())).thenReturn("new-access-token");
        when(refreshTokenService.rotateToken(rawRefreshToken)).thenReturn("rotated-refresh-token");

        AuthSession session = authService.refresh(rawRefreshToken);

        assertThat(session.authResponse().getToken()).isEqualTo("new-access-token");
        assertThat(session.authResponse().getEmail()).isEqualTo(user.getEmail());
        assertThat(session.authResponse().getRole()).isEqualTo(user.getRole().name());
        assertThat(session.authResponse().getUserId()).isEqualTo(user.getId());
        assertThat(session.refreshToken()).isEqualTo("rotated-refresh-token");

        verify(refreshTokenService).validateToken(rawRefreshToken);
        verify(refreshTokenService).rotateToken(rawRefreshToken);
        verify(jwtService).generateToken(user.getId(), user.getRole().name());
    }

    @Test
    void refreshShouldThrowWhenRefreshTokenReferencesMissingUser() {
        String rawRefreshToken = "incoming-refresh-token";
        RefreshToken persistedRefreshToken = RefreshToken.builder()
                .id(11L)
                .token("hashed-token")
                .userId(77L)
                .revoked(false)
                .build();

        when(refreshTokenService.validateToken(rawRefreshToken)).thenReturn(persistedRefreshToken);
        when(userRepository.findById(77L)).thenReturn(Optional.empty());

        InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refresh(rawRefreshToken)
        );

        assertThat(exception.getMessage()).isEqualTo("Refresh token references a missing user");
        verify(refreshTokenService, never()).rotateToken(any(String.class));
    }

    @Test
    void forgotPasswordShouldGeneratePinAndPublishEvent() {
        String email = "test@founderlink.com";
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setName("Test User");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordResetPinRepository.save(any(com.founderlink.auth.entity.PasswordResetPin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.founderlink.auth.dto.ForgotPasswordResponse response = authService.forgotPassword(email);

        assertThat(response.getMessage()).isEqualTo("Password reset PIN has been sent to your email");

        verify(passwordResetPinRepository).deleteByEmail(email);
        verify(passwordResetPinRepository).save(passwordResetPinCaptor.capture());
        verify(passwordResetEventPublisher).publishPasswordResetEvent(passwordResetEventCaptor.capture());

        com.founderlink.auth.entity.PasswordResetPin savedPin = passwordResetPinCaptor.getValue();
        assertThat(savedPin.getEmail()).isEqualTo(email);
        assertThat(savedPin.getPin()).matches("\\d{6}");
        assertThat(savedPin.isUsed()).isFalse();
        assertThat(savedPin.getExpiryDate()).isAfter(java.time.LocalDateTime.now().plusMinutes(4));
        assertThat(savedPin.getExpiryDate()).isBefore(java.time.LocalDateTime.now().plusMinutes(6));

        com.founderlink.auth.dto.PasswordResetEmailEvent event = passwordResetEventCaptor.getValue();
        assertThat(event.getEmail()).isEqualTo(email);
        assertThat(event.getUserName()).isEqualTo("Test User");
        assertThat(event.getPin()).matches("\\d{6}");
    }

    @Test
    void forgotPasswordShouldThrowWhenEmailNotFound() {
        String email = "nonexistent@founderlink.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.forgotPassword(email)
        );

        assertThat(exception.getMessage()).isEqualTo("Email not found");
        verify(passwordResetPinRepository, never()).deleteByEmail(any());
        verify(passwordResetPinRepository, never()).save(any());
        verify(passwordResetEventPublisher, never()).publishPasswordResetEvent(any());
    }

    @Test
    void resetPasswordShouldUpdatePasswordSuccessfully() {
        String email = "test@founderlink.com";
        String pin = "123456";
        String newPassword = "NewPassword123";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setPassword("oldEncodedPassword");

        com.founderlink.auth.entity.PasswordResetPin resetPin = com.founderlink.auth.entity.PasswordResetPin.builder()
                .id(1L)
                .email(email)
                .pin(pin)
                .expiryDate(java.time.LocalDateTime.now().plusMinutes(3))
                .used(false)
                .build();

        when(passwordResetPinRepository.findByEmailAndPin(email, pin)).thenReturn(Optional.of(resetPin));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordResetPinRepository.save(any(com.founderlink.auth.entity.PasswordResetPin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.founderlink.auth.dto.ResetPasswordResponse response = authService.resetPassword(email, pin, newPassword);

        assertThat(response.getMessage()).isEqualTo("Password has been reset successfully");

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("newEncodedPassword");

        verify(passwordResetPinRepository).save(passwordResetPinCaptor.capture());
        com.founderlink.auth.entity.PasswordResetPin savedPin = passwordResetPinCaptor.getValue();
        assertThat(savedPin.isUsed()).isTrue();
    }

    @Test
    void resetPasswordShouldThrowWhenPinIsInvalid() {
        String email = "test@founderlink.com";
        String pin = "999999";
        String newPassword = "NewPassword123";

        when(passwordResetPinRepository.findByEmailAndPin(email, pin)).thenReturn(Optional.empty());

        com.founderlink.auth.exception.InvalidPasswordResetPinException exception = assertThrows(
                com.founderlink.auth.exception.InvalidPasswordResetPinException.class,
                () -> authService.resetPassword(email, pin, newPassword)
        );

        assertThat(exception.getMessage()).isEqualTo("Invalid PIN or email");
        verify(userRepository, never()).save(any());
        verify(passwordResetPinRepository, never()).save(any());
    }

    @Test
    void resetPasswordShouldThrowWhenPinIsAlreadyUsed() {
        String email = "test@founderlink.com";
        String pin = "123456";
        String newPassword = "NewPassword123";

        com.founderlink.auth.entity.PasswordResetPin resetPin = com.founderlink.auth.entity.PasswordResetPin.builder()
                .id(1L)
                .email(email)
                .pin(pin)
                .expiryDate(java.time.LocalDateTime.now().plusMinutes(3))
                .used(true)
                .build();

        when(passwordResetPinRepository.findByEmailAndPin(email, pin)).thenReturn(Optional.of(resetPin));

        com.founderlink.auth.exception.UsedPasswordResetPinException exception = assertThrows(
                com.founderlink.auth.exception.UsedPasswordResetPinException.class,
                () -> authService.resetPassword(email, pin, newPassword)
        );

        assertThat(exception.getMessage()).isEqualTo("PIN has already been used");
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPasswordShouldThrowWhenPinIsExpired() {
        String email = "test@founderlink.com";
        String pin = "123456";
        String newPassword = "NewPassword123";

        com.founderlink.auth.entity.PasswordResetPin resetPin = com.founderlink.auth.entity.PasswordResetPin.builder()
                .id(1L)
                .email(email)
                .pin(pin)
                .expiryDate(java.time.LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();

        when(passwordResetPinRepository.findByEmailAndPin(email, pin)).thenReturn(Optional.of(resetPin));

        com.founderlink.auth.exception.ExpiredPasswordResetPinException exception = assertThrows(
                com.founderlink.auth.exception.ExpiredPasswordResetPinException.class,
                () -> authService.resetPassword(email, pin, newPassword)
        );

        assertThat(exception.getMessage()).isEqualTo("PIN has expired");
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPasswordShouldThrowWhenUserNotFound() {
        String email = "test@founderlink.com";
        String pin = "123456";
        String newPassword = "NewPassword123";

        com.founderlink.auth.entity.PasswordResetPin resetPin = com.founderlink.auth.entity.PasswordResetPin.builder()
                .id(1L)
                .email(email)
                .pin(pin)
                .expiryDate(java.time.LocalDateTime.now().plusMinutes(3))
                .used(false)
                .build();

        when(passwordResetPinRepository.findByEmailAndPin(email, pin)).thenReturn(Optional.of(resetPin));
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.resetPassword(email, pin, newPassword)
        );

        assertThat(exception.getMessage()).isEqualTo("User not found");
        verify(passwordEncoder, never()).encode(any());
    }
}
