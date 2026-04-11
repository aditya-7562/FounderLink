package com.founderlink.notification.client;

import com.founderlink.notification.dto.UserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientFallbackTest {

    private final UserServiceClientFallback userFallback = new UserServiceClientFallback();
    private final StartupServiceClientFallback startupFallback = new StartupServiceClientFallback();

    @Test
    @DisplayName("UserServiceClientFallback - returns empty/null")
    void testUserFallback() {
        assertThat(userFallback.getAllUsers()).isEmpty();
        assertThat(userFallback.getUsersByRole("ADMIN")).isEmpty();
        assertThat(userFallback.getUserById(1L)).isNull();
    }

    @Test
    @DisplayName("StartupServiceClientFallback - returns null")
    void testStartupFallback() {
        assertThat(startupFallback.getStartupById(1L)).isNull();
    }
}
