package com.founderlink.User_Service.dto;

import com.founderlink.User_Service.entity.Role;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class UserResponseDto {

    @JsonProperty("userId")
    private Long id;
    private String name;
    private String email;
    private Role role;
    private String skills;
    private String experience;
    private String bio;
    private String portfolioLinks;

}
