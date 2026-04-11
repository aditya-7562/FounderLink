package com.founderlink.team.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.founderlink.team.exception.StartupServiceUnavailableException;

class StartupServiceClientFallbackTest {

    private StartupServiceClientFallback fallback;

    @BeforeEach
    void setUp() {
        fallback = new StartupServiceClientFallback();
    }

    @Test
    void getStartupById_throwsStartupServiceUnavailableException() {
        assertThatThrownBy(() -> fallback.getStartupById(101L))
                .isInstanceOf(StartupServiceUnavailableException.class)
                .hasMessageContaining("Startup service is temporarily unavailable. Cannot fetch startup with id: 101");
    }
}
