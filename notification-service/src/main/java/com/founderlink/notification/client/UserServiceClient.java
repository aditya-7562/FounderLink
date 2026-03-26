package com.founderlink.notification.client;

import com.founderlink.notification.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/users")
    List<UserDTO> getAllUsers();

    @GetMapping("/users/role/{role}")
    List<UserDTO> getUsersByRole(@PathVariable("role") String role);

    @GetMapping("/users/{id}")
    UserDTO getUserById(@PathVariable Long id);

}
