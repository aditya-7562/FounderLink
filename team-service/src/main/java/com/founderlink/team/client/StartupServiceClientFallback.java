package com.founderlink.team.client;

import com.founderlink.team.dto.response.StartupResponseDto;
import com.founderlink.team.exception.StartupServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartupServiceClientFallback implements StartupServiceClient {

    @Override
    public StartupResponseDto getStartupById(Long id) {
        log.error("Startup Service is down. Cannot fetch startup with id: {}", id);
        throw new StartupServiceUnavailableException(
                "StartupServiceClient#getStartupById",
                "Startup service is temporarily unavailable. Cannot fetch startup with id: " + id);
    }
}
