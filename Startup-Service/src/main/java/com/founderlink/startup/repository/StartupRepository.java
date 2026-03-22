package com.founderlink.startup.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.founderlink.startup.entity.Startup;
import com.founderlink.startup.entity.StartupStage;

@Repository
public interface StartupRepository
        extends JpaRepository<Startup, Long> {

    // ─────────────────────────────────────────
    // MANDATORY
    // Get active startup by ID
    // Used by FeignClient
    // ─────────────────────────────────────────
    Optional<Startup> findByIdAndIsDeletedFalse(Long id);

    // ─────────────────────────────────────────
    // MANDATORY
    // Get all active startups
    // Used by Investor discovery
    // ─────────────────────────────────────────
    List<Startup> findByIsDeletedFalse();

    // ─────────────────────────────────────────
    // MANDATORY
    // Get active startups by founder
    // Founder sees their own startups
    // ─────────────────────────────────────────
    List<Startup> findByFounderIdAndIsDeletedFalse(
            Long founderId);

    // ─────────────────────────────────────────
    // GOOD TO HAVE
    // Search by industry
    // ─────────────────────────────────────────
    List<Startup> findByIndustryAndIsDeletedFalse(
            String industry);

    // ─────────────────────────────────────────
    // GOOD TO HAVE
    // Search by stage
    // ─────────────────────────────────────────
    List<Startup> findByStageAndIsDeletedFalse(
            StartupStage stage);

    // ─────────────────────────────────────────
    // GOOD TO HAVE
    // Search by industry and stage
    // ─────────────────────────────────────────
    List<Startup> findByIndustryAndStageAndIsDeletedFalse(
            String industry,
            StartupStage stage);

    // ─────────────────────────────────────────
    // GOOD TO HAVE
    // Search by funding goal range
    // ─────────────────────────────────────────
    List<Startup> findByFundingGoalBetweenAndIsDeletedFalse(
            BigDecimal min,
            BigDecimal max);

    // ─────────────────────────────────────────
    // GOOD TO HAVE
    // Search by industry and funding range
    // ─────────────────────────────────────────
    List<Startup> findByIndustryAndFundingGoalBetweenAndIsDeletedFalse(
            String industry,
            BigDecimal min,
            BigDecimal max);
}