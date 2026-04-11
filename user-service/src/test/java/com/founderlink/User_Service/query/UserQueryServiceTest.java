package com.founderlink.User_Service.query;

import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.entity.Role;
import com.founderlink.User_Service.entity.User;
import com.founderlink.User_Service.exceptions.UserNotFoundException;
import com.founderlink.User_Service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UserQueryService userQueryService;

    @Test
    void getUser_Found() {
        User user = new User();
        user.setId(1L);
        UserResponseDto dto = new UserResponseDto();
        dto.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserResponseDto.class)).thenReturn(dto);

        UserResponseDto result = userQueryService.getUser(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getUser_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userQueryService.getUser(1L));
    }

    @Test
    void getAllUsers_List() {
        User user = new User();
        UserResponseDto dto = new UserResponseDto();
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll((Pageable) any())).thenReturn(page);
        when(modelMapper.map(user, UserResponseDto.class)).thenReturn(dto);

        List<UserResponseDto> result = userQueryService.getAllUsers();
        assertThat(result).hasSize(1);
    }

    @Test
    void getUsersByRole_List() {
        User user = new User();
        UserResponseDto dto = new UserResponseDto();
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findByRole(eq(Role.FOUNDER), any(Pageable.class))).thenReturn(page);
        when(modelMapper.map(user, UserResponseDto.class)).thenReturn(dto);

        List<UserResponseDto> result = userQueryService.getUsersByRole(Role.FOUNDER);
        assertThat(result).hasSize(1);
    }

    @Test
    void getAllUsers_Paged() {
        User user = new User();
        UserResponseDto dto = new UserResponseDto();
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(modelMapper.map(user, UserResponseDto.class)).thenReturn(dto);

        Page<UserResponseDto> result = userQueryService.getAllUsers(PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getUsersByRole_Paged() {
        User user = new User();
        UserResponseDto dto = new UserResponseDto();
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findByRole(eq(Role.INVESTOR), any(Pageable.class))).thenReturn(page);
        when(modelMapper.map(user, UserResponseDto.class)).thenReturn(dto);

        Page<UserResponseDto> result = userQueryService.getUsersByRole(Role.INVESTOR, PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void countByRole() {
        when(userRepository.countByRole(Role.FOUNDER)).thenReturn(5L);
        long result = userQueryService.countByRole(Role.FOUNDER);
        assertThat(result).isEqualTo(5L);
    }
}
