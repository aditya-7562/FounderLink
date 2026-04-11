package com.founderlink.auth.controller;

import com.founderlink.auth.entity.User;
import com.founderlink.auth.entity.UserStatus;
import com.founderlink.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminAuthController(userRepository))
                .build();
    }

    @Test
    void updateUserStatusShouldReturnForbiddenWhenNoValidHeadersProvided() throws Exception {
        mockMvc.perform(put("/auth/admin/users/1/status")
                        .param("status", "ACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserStatusShouldUpdateStatusWhenValidRoleProvided() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(put("/auth/admin/users/1/status")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .param("status", "ACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Status updated successfully"));

        verify(userRepository, times(1)).save(argThat(savedUser -> savedUser.getStatus() == UserStatus.ACTIVE));
    }

    @Test
    void updateUserStatusShouldUpdateStatusWhenValidInternalSecretProvided() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(put("/auth/admin/users/1/status")
                        .header("X-Internal-Secret", "trusted-internal-secret-xyz123")
                        .param("status", "SUSPENDED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Status updated successfully"));

        verify(userRepository, times(1)).save(argThat(savedUser -> savedUser.getStatus() == UserStatus.SUSPENDED));
    }

    @Test
    void updateUserStatusShouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/auth/admin/users/99/status")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .param("status", "ACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(userRepository, never()).save(any());
    }
}
