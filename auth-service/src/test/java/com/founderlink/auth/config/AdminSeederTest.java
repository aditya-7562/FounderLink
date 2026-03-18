package com.founderlink.auth.config;

import com.founderlink.auth.entity.Role;
import com.founderlink.auth.entity.User;
import com.founderlink.auth.exception.AdminSeedingException;
import com.founderlink.auth.repository.UserRepository;
import com.founderlink.auth.service.SyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SyncService syncService;

    @InjectMocks
    private AdminSeeder adminSeeder;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void seedAdminShouldCreateAdminSuccessfully() {
        when(userRepository.existsByEmail("admin@founderlink.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1")).thenReturn("encoded-admin-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        doNothing().when(syncService).syncUser(any(User.class));

        adminSeeder.seedAdmin("Super Admin", "admin@founderlink.com", "StrongPass1");

        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedAdmin = userCaptor.getValue();
        assertThat(savedAdmin.getName()).isEqualTo("Super Admin");
        assertThat(savedAdmin.getEmail()).isEqualTo("admin@founderlink.com");
        assertThat(savedAdmin.getPassword()).isEqualTo("encoded-admin-password");
        assertThat(savedAdmin.getRole()).isEqualTo(Role.ADMIN);
        verify(syncService).syncUser(savedAdmin);
    }

    @Test
    void seedAdminShouldSkipWhenAdminAlreadyExists() {
        when(userRepository.existsByEmail("admin@founderlink.com")).thenReturn(true);

        adminSeeder.seedAdmin("Super Admin", "admin@founderlink.com", "StrongPass1");

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(passwordEncoder, syncService);
    }

    @Test
    void seedAdminShouldFailWhenSyncFails() {
        when(userRepository.existsByEmail("admin@founderlink.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1")).thenReturn("encoded-admin-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(15L);
            return user;
        });
        doThrow(new IllegalStateException("user-service unavailable")).when(syncService).syncUser(any(User.class));

        AdminSeedingException exception = assertThrows(
                AdminSeedingException.class,
                () -> adminSeeder.seedAdmin("Super Admin", "admin@founderlink.com", "StrongPass1")
        );

        assertThat(exception.getMessage()).isEqualTo("Failed to sync admin with user-service");
        verify(userRepository).saveAndFlush(any(User.class));
        verify(syncService).syncUser(any(User.class));
    }
}
