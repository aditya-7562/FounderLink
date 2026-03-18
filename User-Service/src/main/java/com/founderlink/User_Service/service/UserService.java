package com.founderlink.User_Service.service;

import com.founderlink.User_Service.dto.UserRequestAuthDto;
import com.founderlink.User_Service.dto.UserRequestDto;
import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.entity.User;
import com.founderlink.User_Service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final ModelMapper modelMapper;

    public UserResponseDto getUser(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User Not Found."));
        return modelMapper.map(user, UserResponseDto.class);
    }

    // Just for testing
    public UserResponseDto createUser(UserRequestAuthDto dto) {

        // ✅ TRUE idempotency
        User existing = repository.findById(dto.getUserId()).orElse(null);

        if (existing != null) {
            return modelMapper.map(existing, UserResponseDto.class);
        }

        User user = new User();

        user.setId(dto.getUserId()); // 🔥 critical

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setSkills(dto.getSkills());
        user.setExperience(dto.getExperience());
        user.setBio(dto.getBio());
        user.setPortfolioLinks(dto.getPortfolioLinks());
        user.setUpdatedAt(LocalDateTime.now());

        return modelMapper.map(repository.save(user), UserResponseDto.class);
    }

    public List<UserResponseDto> getAllUsers() {
        return repository.findAll()
                .stream()
                .map(u -> modelMapper.map(u, UserResponseDto.class))
                .toList();
    }

    public UserResponseDto updateUser(Long id, UserRequestDto dto) {
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User Not Found."));

        modelMapper.map(dto, user);
        user.setUpdatedAt(LocalDateTime.now());

        return modelMapper.map(repository.save(user), UserResponseDto.class);
    }


}
