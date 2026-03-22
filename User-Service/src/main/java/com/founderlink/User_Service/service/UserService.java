package com.founderlink.User_Service.service;

import com.founderlink.User_Service.dto.UserRequestAuthDto;
import com.founderlink.User_Service.dto.UserRequestDto;
import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.entity.User;
import com.founderlink.User_Service.exceptions.ConflictException;
import com.founderlink.User_Service.exceptions.UserNotFoundException;
import com.founderlink.User_Service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final ModelMapper modelMapper;

    public UserResponseDto getUser(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found."));
        return modelMapper.map(user, UserResponseDto.class);
    }


    public UserResponseDto createUser(UserRequestAuthDto dto) {


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

    public List<UserResponseDto> getAllUsers() {
        return repository.findAll()
                .stream()
                .map(u -> modelMapper.map(u, UserResponseDto.class))
                .toList();
    }

    public UserResponseDto updateUser(Long id, UserRequestDto dto) {
        User user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        if (dto.getName() != null) {
            user.setName(dto.getName());
        }
        if (dto.getSkills() != null) {
            user.setSkills(dto.getSkills());
        }
        if (dto.getExperience() != null) {
            user.setExperience(dto.getExperience());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }
        if (dto.getPortfolioLinks() != null) {
            user.setPortfolioLinks(dto.getPortfolioLinks());
        }
        user.setUpdatedAt(LocalDateTime.now());

        return modelMapper.map(repository.save(user), UserResponseDto.class);
    }


}
