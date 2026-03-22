package com.founderlink.notification.client;

import com.founderlink.notification.dto.StartupDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StartupServiceClientFallback implements StartupServiceClient {

    private static final Logger log = LoggerFactory.getLogger(StartupServiceClientFallback.class);

    @Override
    public StartupDTO getStartupById(Long id) {
        log.warn("Startup-service is unavailable. Fallback triggered for getStartupById: {}", id);
        return null;
    }
}
