package com.founderlink.team.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.founderlink.team.entity.TeamMember;
import com.founderlink.team.entity.TeamRole;

@Repository
public interface TeamMemberRepository
        extends JpaRepository<TeamMember, Long> {

    // ─────────────────────────────────────────
    // MANDATORY
    // Get ACTIVE members of a startup
    // Updated → isActiveTrue
    // ─────────────────────────────────────────
    List<TeamMember> findByStartupIdAndIsActiveTrue(
            Long startupId);
    Page<TeamMember> findByStartupIdAndIsActiveTrue(
            Long startupId,
            Pageable pageable);

    // ─────────────────────────────────────────
    // MANDATORY
    // Check if user is ACTIVE member
    // Updated → isActiveTrue
    // Prevents duplicate membership
    // ─────────────────────────────────────────
    boolean existsByStartupIdAndUserIdAndIsActiveTrue(
            Long startupId,
            Long userId);

    // ─────────────────────────────────────────
    // GOOD TO HAVE
    // Check if ACTIVE role exists
    // Updated → isActiveTrue
    // Prevents duplicate roles
    // ─────────────────────────────────────────
    boolean existsByStartupIdAndRoleAndIsActiveTrue(
            Long startupId,
            TeamRole role);

    // ─────────────────────────────────────────
    // GOOD TO HAVE
    // Find specific ACTIVE member
    // Updated → isActiveTrue
    // Used in DELETE operation
    // ─────────────────────────────────────────
    Optional<TeamMember> findByStartupIdAndUserIdAndIsActiveTrue(
            Long startupId,
            Long userId);

    // ─────────────────────────────────────────
    // NEW
    // Get full work history of user
    // ALL records active + inactive
    // ─────────────────────────────────────────
    List<TeamMember> findByUserId(Long userId);
    Page<TeamMember> findByUserId(Long userId, Pageable pageable);

    // ─────────────────────────────────────────
    // NEW
    // Get current active roles of user
    // Only active records
    // ─────────────────────────────────────────
    List<TeamMember> findByUserIdAndIsActiveTrue(
            Long userId);
    Page<TeamMember> findByUserIdAndIsActiveTrue(
            Long userId,
            Pageable pageable);

    // ─────────────────────────────────────────
    // NEW
    // Get ALL members of startup
    // active + inactive
    // Used for history
    // ─────────────────────────────────────────
    List<TeamMember> findByStartupId(Long startupId);
}
