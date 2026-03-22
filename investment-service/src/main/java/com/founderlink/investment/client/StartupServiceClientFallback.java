package com.founderlink.investment.client;

import org.springframework.stereotype.Component;

import com.founderlink.investment.dto.response.StartupResponseDto;
import com.founderlink.investment.exception.StartupNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StartupServiceClientFallback
        implements StartupServiceClient {

    @Override
    public StartupResponseDto getStartupById(Long id) {
        log.error("Startup Service is down. " +
                "Cannot fetch startup with id: {}", id);
        throw new StartupNotFoundException(
                "Startup service is unavailable. Cannot fetch startup with id: " + id);
    }
}