package com.founderlink.User_Service.dto;

import lombok.Data;


@Data
public class UserRequestDto {

    private String name;
    private String skills;
    private String experience;
    private String bio;
    private String portfolioLinks;
}
