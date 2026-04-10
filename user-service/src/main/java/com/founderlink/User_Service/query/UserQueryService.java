package com.founderlink.User_Service.query;

import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.entity.Role;
import com.founderlink.User_Service.exceptions.UserNotFoundException;
import com.founderlink.User_Service.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository repository;
    private final ModelMapper modelMapper;

    /**
     * QUERY: Get single user by ID.
     * Cache key = userId. Cache hit → skip DB.
     */
    @Cacheable(value = "userById", key = "#id")
    public UserResponseDto getUser(Long id) {
        log.info("QUERY - getUser: id={} (cache miss, hitting DB)", id);
        return repository.findById(id)
                .map(u -> modelMapper.map(u, UserResponseDto.class))
                .orElseThrow(() -> new UserNotFoundException("User not found."));
    }

    /**
     * QUERY: Get all users.
     * Single shared cache entry — invalidated on any write.
     */
    @Cacheable(value = "allUsers", key = "'all'")
    public List<UserResponseDto> getAllUsers() {
        log.info("QUERY - getAllUsers (cache miss, hitting DB)");
        return getAllUsers(Pageable.unpaged()).getContent();
    }

    /**
     * QUERY: Get users filtered by role.
     * Cache key = role name.
     */
    @Cacheable(value = "usersByRole", key = "#role.name()")
    public List<UserResponseDto> getUsersByRole(Role role) {
        log.info("QUERY - getUsersByRole: role={} (cache miss, hitting DB)", role);
        return getUsersByRole(role, Pageable.unpaged()).getContent();
    }

    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        log.info("QUERY - getAllUsers: pageable={}", pageable);
        return repository.findAll(pageable)
                .map(user -> modelMapper.map(user, UserResponseDto.class));
    }

    public Page<UserResponseDto> getUsersByRole(Role role, Pageable pageable) {
        log.info("QUERY - getUsersByRole: role={}, pageable={}", role, pageable);
        return repository.findByRole(role, pageable)
                .map(user -> modelMapper.map(user, UserResponseDto.class));
    }

    
    /**
     * QUERY: Count users by role.
     */
    @Cacheable(value = "userCountByRole", key = "#role.name()")
    public long countByRole(Role role) {
        log.info("QUERY - countByRole: role={} (cache miss, hitting DB)", role);
        return repository.countByRole(role);
    }
}
