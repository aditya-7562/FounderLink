package com.founderlink.auth.service;

import com.founderlink.auth.client.UserClient;
import com.founderlink.auth.dto.UserRequest;
import com.founderlink.auth.entity.User;
import com.founderlink.auth.exception.UserServiceClientException;
import com.founderlink.auth.exception.UserServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final UserClient userClient;

    @Retry(name = "userServiceSync", fallbackMethod = "syncUserFallback")
    @CircuitBreaker(name = "userServiceSync")
    public void syncUser(User user) {
        UserRequest request = new UserRequest();
        request.setUserId(user.getId());
        request.setName(user.getName());
        request.setEmail(user.getEmail());

        log.debug("Syncing user to user-service");

        userClient.createUser(request);

        log.debug("User synced successfully to user-service");
    }

    public void syncUserFallback(User user, Throwable throwable) {

        if (throwable instanceof UserServiceClientException clientException) {
            log.warn("User-service rejected request. method={} status={}",
                    clientException.getMethodKey(),
                    clientException.getStatus());
            throw clientException;
        }

        log.error("User-service sync failed (retry exhausted or circuit open)", throwable);

        throw new UserServiceUnavailableException(
                "UserClient#createUser",
                "User service is temporarily unavailable"
        );
    }
}
