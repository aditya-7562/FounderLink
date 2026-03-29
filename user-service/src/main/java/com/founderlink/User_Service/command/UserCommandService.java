package com.founderlink.User_Service.command;

import java.time.LocalDateTime;
import java.util.Objects;

import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.founderlink.User_Service.dto.UserRequestAuthDto;
import com.founderlink.User_Service.dto.UserRequestDto;
import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.entity.User;
import com.founderlink.User_Service.exceptions.ConflictException;
import com.founderlink.User_Service.exceptions.UserNotFoundException;
import com.founderlink.User_Service.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository repository;
    private final ModelMapper modelMapper;

    /**
     * COMMAND: Sync user from auth-service (idempotent upsert).
     * Evicts userById and allUsers caches on write.
     */
    @Caching(evict = {
        @CacheEvict(value = "userById",    key = "#dto.userId"),
        @CacheEvict(value = "allUsers",    allEntries = true),
        @CacheEvict(value = "usersByRole", allEntries = true)
    })
    public UserResponseDto createUser(UserRequestAuthDto dto) {
        log.info("COMMAND - createUser: userId={}", dto.getUserId());

        User existing = repository.findById(dto.getUserId()).orElse(null);

        if (existing != null) {
            if (!Objects.equals(existing.getEmail(), dto.getEmail())
                    || !Objects.equals(existing.getRole(), dto.getRole())) {
                throw new ConflictException("User identity data does not match existing record.");
            }
            return modelMapper.map(existing, UserResponseDto.class);
        }

        User user = new User();
        user.setId(dto.getUserId());
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        user.setSkills(dto.getSkills());
        user.setExperience(dto.getExperience());
        user.setBio(dto.getBio());
        user.setPortfolioLinks(dto.getPortfolioLinks());
        user.setUpdatedAt(LocalDateTime.now());

        return modelMapper.map(repository.save(user), UserResponseDto.class);
    }

    /**
     * COMMAND: Update user profile fields.
     * Evicts userById, allUsers, and usersByRole caches.
     */
    @Caching(evict = {
        @CacheEvict(value = "userById",    key = "#id"),
        @CacheEvict(value = "allUsers",    allEntries = true),
        @CacheEvict(value = "usersByRole", allEntries = true)
    })
    public UserResponseDto updateUser(Long id, UserRequestDto dto) {
        log.info("COMMAND - updateUser: userId={}", id);

        User user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        if (dto.getName() != null)           user.setName(dto.getName());
        if (dto.getSkills() != null)         user.setSkills(dto.getSkills());
        if (dto.getExperience() != null)     user.setExperience(dto.getExperience());
        if (dto.getBio() != null)            user.setBio(dto.getBio());
        if (dto.getPortfolioLinks() != null) user.setPortfolioLinks(dto.getPortfolioLinks());
        user.setUpdatedAt(LocalDateTime.now());

        return modelMapper.map(repository.save(user), UserResponseDto.class);
    }
}
