package com.founderlink.User_Service.controller;

import com.founderlink.User_Service.dto.UserRequestAuthDto;
import com.founderlink.User_Service.dto.UserRequestDto;
import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;


    // Just for Testing
    @PostMapping("/internal")
    public ResponseEntity<UserResponseDto> createUser(
            @Valid @RequestBody UserRequestAuthDto dto) {
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
}
