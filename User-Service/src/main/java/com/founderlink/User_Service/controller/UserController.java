package com.founderlink.User_Service.controller;

import com.founderlink.User_Service.dto.UserRequestAuthDto;
import com.founderlink.User_Service.dto.UserRequestDto;
import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @Value("${internal.secret:my-founderlink-internal-secret-2024}")
    private String internalSecret;

    private static final String AUTH_SOURCE_HEADER = "X-Auth-Source";
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    private static final String EXPECTED_AUTH_SOURCE = "gateway";

    // Adding Users
    @PostMapping("/internal")
    public ResponseEntity<UserResponseDto> createUser(
            @Valid @RequestBody UserRequestAuthDto dto,
            @RequestHeader(name = AUTH_SOURCE_HEADER, required = false) String authSource,
            @RequestHeader(name = INTERNAL_SECRET_HEADER, required = false) String secret) {
        
        // Validate internal endpoint access
        if (!isValidInternalAccess(authSource, secret)) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(service.createUser(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable Long id){
        return ResponseEntity.ok(service.getUser(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @RequestBody UserRequestDto userRequestDto){
        return ResponseEntity.ok(service.updateUser(id, userRequestDto));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers(){
        return ResponseEntity.ok(service.getAllUsers());
    }

    private boolean isValidInternalAccess(String authSource, String secret) {
        // Both headers must be present and correct
        if (authSource == null || secret == null) {
            return false;
        }
        
        return EXPECTED_AUTH_SOURCE.equals(authSource) && internalSecret.equals(secret);
    }
}
