package com.founderlink.User_Service.command;

import com.founderlink.User_Service.dto.UserRequestAuthDto;
import com.founderlink.User_Service.dto.UserRequestDto;
import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.entity.Role;
import com.founderlink.User_Service.entity.User;
import com.founderlink.User_Service.exceptions.ConflictException;
import com.founderlink.User_Service.exceptions.UserNotFoundException;
import com.founderlink.User_Service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UserCommandService userCommandService;

    @Test
    void createUser_NewUser_Success() {
        UserRequestAuthDto dto = new UserRequestAuthDto();
        dto.setUserId(1L);
        dto.setEmail("test@test.com");
        dto.setRole(Role.FOUNDER);
        dto.setName("Name");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(modelMapper.map(any(User.class), eq(UserResponseDto.class))).thenReturn(new UserResponseDto());

        userCommandService.createUser(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void createUser_ExistingUser_Matches_ReturnsExisting() {
        UserRequestAuthDto dto = new UserRequestAuthDto();
        dto.setUserId(1L);
        dto.setEmail("test@test.com");
        dto.setRole(Role.FOUNDER);

        User existing = new User();
        existing.setEmail("test@test.com");
        existing.setRole(Role.FOUNDER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(modelMapper.map(existing, UserResponseDto.class)).thenReturn(new UserResponseDto());

        userCommandService.createUser(dto);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_ExistingUser_EmailMismatch_ThrowsConflict() {
        UserRequestAuthDto dto = new UserRequestAuthDto();
        dto.setUserId(1L);
        dto.setEmail("new@test.com");
        dto.setRole(Role.FOUNDER);

        User existing = new User();
        existing.setEmail("old@test.com");
        existing.setRole(Role.FOUNDER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> userCommandService.createUser(dto));
    }

    @Test
    void createUser_ExistingUser_RoleMismatch_ThrowsConflict() {
        UserRequestAuthDto dto = new UserRequestAuthDto();
        dto.setUserId(1L);
        dto.setEmail("test@test.com");
        dto.setRole(Role.FOUNDER);

        User existing = new User();
        existing.setEmail("test@test.com");
        existing.setRole(Role.INVESTOR);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> userCommandService.createUser(dto));
    }

    @Test
    void updateUser_Success() {
        UserRequestDto dto = new UserRequestDto();
        dto.setName("New Name");
        dto.setSkills("New Skills");
        dto.setExperience("New Exp");
        dto.setBio("New Bio");
        dto.setPortfolioLinks("New Links");

        User existing = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(modelMapper.map(existing, UserResponseDto.class)).thenReturn(new UserResponseDto());

        userCommandService.updateUser(1L, dto);

        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getSkills()).isEqualTo("New Skills");
        assertThat(existing.getExperience()).isEqualTo("New Exp");
        assertThat(existing.getBio()).isEqualTo("New Bio");
        assertThat(existing.getPortfolioLinks()).isEqualTo("New Links");
        verify(userRepository).save(existing);
    }

    @Test
    void updateUser_NotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userCommandService.updateUser(1L, new UserRequestDto()));
    }
}
