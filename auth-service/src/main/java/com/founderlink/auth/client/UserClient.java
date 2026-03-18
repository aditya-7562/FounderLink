package com.founderlink.auth.client;

import com.founderlink.auth.config.FeignConfig;
import com.founderlink.auth.dto.UserRequest;
import com.founderlink.auth.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", path = "/users", configuration = FeignConfig.class)
public interface UserClient {

    @PostMapping("/internal")
    ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request);
}
