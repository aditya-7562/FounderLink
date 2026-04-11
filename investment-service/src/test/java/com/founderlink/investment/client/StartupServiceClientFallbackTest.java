package com.founderlink.investment.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.founderlink.investment.dto.response.StartupResponseDto;
import com.founderlink.investment.exception.StartupServiceUnavailableException;

class StartupServiceClientFallbackTest {

    private final StartupServiceClientFallback fallback = new StartupServiceClientFallback();

    @Test
    void getStartupById_ThrowsException() {
        assertThatThrownBy(() -> fallback.getStartupById(101L))
                .isInstanceOf(StartupServiceUnavailableException.class)
                .hasMessageContaining("Startup service is temporarily unavailable");
    }
}
