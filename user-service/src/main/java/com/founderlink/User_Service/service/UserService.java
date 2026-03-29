package com.founderlink.User_Service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.founderlink.User_Service.command.UserCommandService;
import com.founderlink.User_Service.dto.UserRequestAuthDto;
import com.founderlink.User_Service.dto.UserRequestDto;
import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.entity.Role;
import com.founderlink.User_Service.query.UserQueryService;

import lombok.RequiredArgsConstructor;

/**
 * Facade that preserves the existing UserService contract.
 * Delegates writes → UserCommandService (CQRS Command side)
 * Delegates reads  → UserQueryService   (CQRS Query side + Redis cache)
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserCommandService commandService;
    private final UserQueryService   queryService;

    public UserResponseDto createUser(UserRequestAuthDto dto) {
        return commandService.createUser(dto);
    }

    public UserResponseDto updateUser(Long id, UserRequestDto dto) {
        return commandService.updateUser(id, dto);
    }

    public UserResponseDto getUser(Long id) {
        return queryService.getUser(id);
    }

    public List<UserResponseDto> getAllUsers() {
        return queryService.getAllUsers();
    }

    public List<UserResponseDto> getUsersByRole(Role role) {
        return queryService.getUsersByRole(role);
    }
}
