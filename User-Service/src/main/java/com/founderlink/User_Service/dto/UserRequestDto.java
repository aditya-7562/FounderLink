package com.founderlink.User_Service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class UserRequestDto {

    @NotNull(message = "Id is Required")
    private Long userId;

    @NotBlank(message = "Name is Required")
    private String name;

    @Email(message = "Invalid Email")
    @NotBlank(message = "Email is Required")
    private String email;

    @NotBlank(message = "Skill is Required")
    private String skills;

    @NotBlank(message = "Experience is Required")
    private String experience;

    @NotBlank(message = "Bio is Required")
    private String bio;

    @NotBlank(message = "Portfolio Link is Required")
    private String portfolioLinks;
}
