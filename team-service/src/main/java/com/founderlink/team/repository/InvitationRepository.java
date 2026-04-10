package com.founderlink.team.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.entity.TeamRole;

@Repository
public interface InvitationRepository
        extends JpaRepository<Invitation, Long> {


    // Edge case → prevent duplicate invitations
    // Same person invited twice to same startup
	
    boolean existsByStartupIdAndInvitedUserIdAndStatus(
            Long startupId,
            Long invitedUserId,
            InvitationStatus status);

    // Used in join operation
    // Find PENDING invitation before accepting
    // No invitation = cannot join
    
    Optional<Invitation> findByStartupIdAndInvitedUserIdAndStatus(
            Long startupId,
            Long invitedUserId,
            InvitationStatus status);

    // Edge case → prevent same role invited twice
    // CTO already invited → block another CTO invite
    
    boolean existsByStartupIdAndRoleAndStatus(
            Long startupId,
            TeamRole role,
            InvitationStatus status);

    // Get all invitations for a startup
    // Founder sees all pending invitations
    
    List<Invitation> findByStartupId(Long startupId);
    Page<Invitation> findByStartupId(Long startupId, Pageable pageable);


    // Get all pending invitations for a user
    // Co-founder sees who invited them
    
    List<Invitation> findByInvitedUserIdAndStatus(
            Long invitedUserId,
            InvitationStatus status);
    
    List<Invitation> findByInvitedUserId(
            Long invitedUserId);

    Page<Invitation> findByInvitedUserId(
            Long invitedUserId,
            Pageable pageable);
}
