package com.founderlink.User_Service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private Long id;

    private String name;

    @Column(nullable = false, unique = true)
    private String email;
    private String skills;
    private String experience;
    private String bio;
    private String portfolioLinks;

    @Enumerated(EnumType.STRING)
    private Role role;

    private LocalDateTime updatedAt;
}
