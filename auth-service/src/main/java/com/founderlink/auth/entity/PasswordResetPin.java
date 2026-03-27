package com.founderlink.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "password_reset_pins",
        indexes = {
                @Index(name = "idx_password_reset_pins_email", columnList = "email"),
                @Index(name = "idx_password_reset_pins_expiry", columnList = "expiry_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetPin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 6)
    private String pin;

    @Column(nullable = false)
    private String email;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
