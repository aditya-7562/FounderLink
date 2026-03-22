package com.founderlink.auth.dto;

import com.founderlink.auth.entity.Role;
import lombok.Data;

@Data
public class UserRequest {
    private Long userId;
    private String name;
    private String email;
    private Role role;
}