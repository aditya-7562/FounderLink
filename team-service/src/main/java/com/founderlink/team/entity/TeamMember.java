package com.founderlink.team.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "team_members",
        indexes = {
                @Index(name = "idx_team_members_startup_active", columnList = "startupId, isActive"),
                @Index(name = "idx_team_members_user_active", columnList = "userId, isActive"),
                @Index(name = "idx_team_members_joined_at", columnList = "joinedAt")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long startupId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamRole role;
    
    @Column(nullable = false)
    private Boolean isActive = true;

    private LocalDateTime leftAt;

    @Column(updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
        this.isActive = true;              // ← NEW
        this.leftAt = null;               
    }
}
