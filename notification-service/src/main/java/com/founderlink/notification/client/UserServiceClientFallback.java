package com.founderlink.notification.client;

import com.founderlink.notification.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class UserServiceClientFallback implements UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClientFallback.class);

    @Override
    public List<UserDTO> getAllUsers() {
        log.warn("User-service is unavailable. Fallback triggered for getAllUsers");
        return Collections.emptyList();
    }
    
    @Override
    public UserDTO getUserById(Long id) {
        log.warn("User-service is unavailable. Fallback triggered for getUserById: {}", id);
        return null;
    }

}
