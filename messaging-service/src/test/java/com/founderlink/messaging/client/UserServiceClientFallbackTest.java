package com.founderlink.messaging.client;

import com.founderlink.messaging.dto.UserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceClientFallbackTest {

    private final UserServiceClientFallback fallback = new UserServiceClientFallback();

    @Test
    @DisplayName("getUserById - returns null on fallback")
    void getUserById_ReturnsNull() {
        UserDTO result = fallback.getUserById(1L);
        assertThat(result).isNull();
    }
}
