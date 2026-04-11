package com.founderlink.User_Service.service;

import com.founderlink.User_Service.command.UserCommandService;
import com.founderlink.User_Service.dto.UserRequestAuthDto;
import com.founderlink.User_Service.dto.UserRequestDto;
import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.entity.Role;
import com.founderlink.User_Service.query.UserQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserCommandService commandService;

    @Mock
    private UserQueryService queryService;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser() {
        UserRequestAuthDto req = new UserRequestAuthDto();
        UserResponseDto res = new UserResponseDto();
        when(commandService.createUser(req)).thenReturn(res);
        assertThat(userService.createUser(req)).isEqualTo(res);
    }

    @Test
    void updateUser() {
        UserRequestDto req = new UserRequestDto();
        UserResponseDto res = new UserResponseDto();
        when(commandService.updateUser(1L, req)).thenReturn(res);
        assertThat(userService.updateUser(1L, req)).isEqualTo(res);
    }

    @Test
    void getUser() {
        UserResponseDto res = new UserResponseDto();
        when(queryService.getUser(1L)).thenReturn(res);
        assertThat(userService.getUser(1L)).isEqualTo(res);
    }

    @Test
    void getAllUsers_List() {
        List<UserResponseDto> res = List.of(new UserResponseDto());
        when(queryService.getAllUsers()).thenReturn(res);
        assertThat(userService.getAllUsers()).isEqualTo(res);
    }

    @Test
    void getAllUsers_Paged() {
        Page<UserResponseDto> page = mock(Page.class);
        Pageable p = mock(Pageable.class);
        when(queryService.getAllUsers(p)).thenReturn(page);
        assertThat(userService.getAllUsers(p)).isEqualTo(page);
    }

    @Test
    void getUsersByRole_List() {
        List<UserResponseDto> res = List.of(new UserResponseDto());
        when(queryService.getUsersByRole(Role.FOUNDER)).thenReturn(res);
        assertThat(userService.getUsersByRole(Role.FOUNDER)).isEqualTo(res);
    }

    @Test
    void getUsersByRole_Paged() {
        Page<UserResponseDto> page = mock(Page.class);
        Pageable p = mock(Pageable.class);
        when(queryService.getUsersByRole(Role.FOUNDER, p)).thenReturn(page);
        assertThat(userService.getUsersByRole(Role.FOUNDER, p)).isEqualTo(page);
    }

    @Test
    void countByRole() {
        when(queryService.countByRole(Role.INVESTOR)).thenReturn(10L);
        assertThat(userService.countByRole(Role.INVESTOR)).isEqualTo(10L);
    }
}
