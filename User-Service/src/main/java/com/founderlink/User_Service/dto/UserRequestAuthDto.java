package com.founderlink.User_Service.dto;

import com.founderlink.User_Service.entity.Role;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRequestAuthDto {

    @NotNull
    private Long userId;

    private String name;

    @Email(message = "Invalid Email")
    @NotBlank(message = "Email is Required")
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String skills;
    private String experience;
    private String bio;
    private String portfolioLinks;
}
