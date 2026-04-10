package com.founderlink.startup.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Page<Startup> findByIsDeletedFalse(Pageable pageable);

    // ─────────────────────────────────────────
    // MANDATORY
    // Get active startups by founder
    // Founder sees their own startups
    // ─────────────────────────────────────────
    List<Startup> findByFounderIdAndIsDeletedFalse(
            Long founderId);

    Page<Startup> findByFounderIdAndIsDeletedFalse(
            Long founderId,
            Pageable pageable);

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

    @Query("""
            SELECT s FROM Startup s
            WHERE s.isDeleted = false
              AND (:industry IS NULL OR s.industry = :industry)
              AND (:stage IS NULL OR s.stage = :stage)
              AND (:minFunding IS NULL OR s.fundingGoal >= :minFunding)
              AND (:maxFunding IS NULL OR s.fundingGoal <= :maxFunding)
            """)
    Page<Startup> searchActiveStartups(
            @Param("industry") String industry,
            @Param("stage") StartupStage stage,
            @Param("minFunding") BigDecimal minFunding,
            @Param("maxFunding") BigDecimal maxFunding,
            Pageable pageable);

    @Query("SELECT COUNT(s) FROM Startup s WHERE s.isDeleted = false")
    long countActiveStartups();

    @Query("SELECT SUM(s.fundingGoal) FROM Startup s WHERE s.isDeleted = false")
    BigDecimal sumActiveFundingGoal();
}
