package com.founderlink.team.client;

import com.founderlink.team.dto.response.StartupResponseDto;
import com.founderlink.team.exception.StartupNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
