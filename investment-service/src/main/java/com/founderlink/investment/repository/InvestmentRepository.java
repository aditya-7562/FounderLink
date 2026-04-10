package com.founderlink.investment.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.founderlink.investment.entity.Investment;
import com.founderlink.investment.entity.InvestmentStatus;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    // Get all investments for a specific startup
    List<Investment> findByStartupId(Long startupId);
    Page<Investment> findByStartupId(Long startupId, Pageable pageable);

    // Get all investments by a specific investor
    List<Investment> findByInvestorId(Long investorId);
    Page<Investment> findByInvestorId(Long investorId, Pageable pageable);

    // Get investments by startup and status
    List<Investment> findByStartupIdAndStatus(Long startupId, InvestmentStatus status);

    // Get investments by investor and status
    List<Investment> findByInvestorIdAndStatus(Long investorId, InvestmentStatus status);

    // Check if investor already invested in same startup
    boolean existsByStartupIdAndInvestorId(Long startupId, Long investorId);

	boolean existsByStartupIdAndInvestorIdAndStatus(Long startupId, Long investorId, InvestmentStatus pending);
}
