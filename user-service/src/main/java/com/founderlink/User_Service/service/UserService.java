package com.founderlink.User_Service.service;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.founderlink.User_Service.command.UserCommandService;
import com.founderlink.User_Service.dto.UserRequestAuthDto;
import com.founderlink.User_Service.dto.UserRequestDto;
import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.entity.Role;
import com.founderlink.User_Service.query.UserQueryService;

import lombok.RequiredArgsConstructor;

import java.util.List;

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

    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        return queryService.getAllUsers(pageable);
    }

    public List<UserResponseDto> getUsersByRole(Role role) {
        return queryService.getUsersByRole(role);
    }

    public Page<UserResponseDto> getUsersByRole(Role role, Pageable pageable) {
        return queryService.getUsersByRole(role, pageable);
    }

    public Page<UserResponseDto> searchUsersByRoleAndKeyword(Role role, String keyword, Pageable pageable) {
        return queryService.searchUsersByRoleAndKeyword(role, keyword, pageable);
    }

    public long countByRole(Role role) {
        return queryService.countByRole(role);
    }
}
