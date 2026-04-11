package com.founderlink.startup.entity;

import java.math.BigDecimal;
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
        name = "startups",
        indexes = {
                @Index(name = "idx_startups_founder_deleted", columnList = "founderId, isDeleted"),
                @Index(name = "idx_startups_stage_deleted", columnList = "stage, isDeleted"),
                @Index(name = "idx_startups_industry_deleted", columnList = "industry, isDeleted"),
                @Index(name = "idx_startups_created_at", columnList = "created_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Startup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String industry;

    @Column(nullable = false)
    private String problemStatement;

    @Column(nullable = false)
    private String solution;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fundingGoal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StartupStage stage;

    // ← Critical for FeignClient
    @Column(nullable = false)
    private Long founderId;

    // ← Startup moderation
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    private ModerationStatus moderationStatus = ModerationStatus.APPROVED;

    @Column(name = "moderation_reason")
    private String moderationReason;

    // ← Soft delete flag
    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", updatable = false, columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.isDeleted = false;
    }
}
