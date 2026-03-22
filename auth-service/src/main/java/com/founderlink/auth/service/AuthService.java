package com.founderlink.auth.service;

import com.founderlink.auth.dto.*;
import com.founderlink.auth.entity.Role;
import com.founderlink.auth.entity.User;
import com.founderlink.auth.exception.EmailAlreadyExistsException;
import com.founderlink.auth.exception.ExpiredRefreshTokenException;
import com.founderlink.auth.exception.InvalidRefreshTokenException;
import com.founderlink.auth.exception.RevokedRefreshTokenException;
import com.founderlink.auth.repository.UserRepository;
import com.founderlink.auth.security.JwtService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    private final AuthenticationManager authenticationManager;
    private final SyncService syncService;

    private static final Set<Role> ALLOWED_SELF_ROLES = Set.of(
            Role.FOUNDER,
            Role.INVESTOR,
            Role.COFOUNDER
    );

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.debug("Processing registration request");

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        Role requestedRole = getRole(request);

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(requestedRole);

        User savedUser = userRepository.saveAndFlush(user);
        log.debug("User persisted in auth-service");

        syncService.syncUser(savedUser);

        return RegisterResponse.builder()
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .message("User registered successfully")
                .build();
    }

    private static @NonNull Role getRole(RegisterRequest request) {
        Role requestedRole = request.getRole();

        if (requestedRole == null) {
            throw new IllegalArgumentException("Role is required");
        }

        if (requestedRole == Role.ADMIN) {
            throw new AccessDeniedException("Requested role is not allowed");
        }

        if (!ALLOWED_SELF_ROLES.contains(requestedRole)) {
            throw new IllegalArgumentException("Invalid role selection");
        }
        return requestedRole;
    }

    public AuthSession login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        String refreshToken = refreshTokenService.createToken(user.getId());

        return new AuthSession(buildAuthResponse(user, token), refreshToken);
    }

    public AuthSession refresh(String refreshToken) {
        var persistedRefreshToken = refreshTokenService.validateToken(refreshToken);

        User user = userRepository.findById(persistedRefreshToken.getUserId())
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token references a missing user"));

        log.debug("Accepted refresh token usage");

        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        String rotatedRefreshToken = refreshTokenService.rotateToken(refreshToken);

        return new AuthSession(buildAuthResponse(user, accessToken), rotatedRefreshToken);
    }

    public void logout(String refreshToken) {
        try {
            refreshTokenService.revokeToken(refreshToken);
        } catch (InvalidRefreshTokenException |
                 RevokedRefreshTokenException |
                 ExpiredRefreshTokenException ex) {

            log.debug("Logout no-op: {}", ex.getMessage());
        }
    }

    private AuthResponse buildAuthResponse(User user, String accessToken) {
        return AuthResponse.builder()
                .token(accessToken)
                .email(user.getEmail())
                .role(user.getRole().name())
                .userId(user.getId())
                .build();
    }
}
