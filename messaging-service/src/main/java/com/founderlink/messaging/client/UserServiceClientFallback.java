package com.founderlink.messaging.client;

import com.founderlink.messaging.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserServiceClientFallback implements UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClientFallback.class);

    @Override
    public UserDTO getUserById(Long id) {
        log.warn("User-service is unavailable. Fallback triggered for user ID: {}", id);
        return null;
    }
}
