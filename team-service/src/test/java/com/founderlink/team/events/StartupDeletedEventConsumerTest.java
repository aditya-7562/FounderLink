package com.founderlink.team.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.entity.TeamMember;
import com.founderlink.team.repository.InvitationRepository;
import com.founderlink.team.repository.TeamMemberRepository;

@ExtendWith(MockitoExtension.class)
class StartupDeletedEventConsumerTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private StartupDeletedEventConsumer consumer;

    private StartupDeletedEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new StartupDeletedEvent();
        testEvent.setStartupId(101L);
    }

    @Test
    void handleStartupDeletedEvent_Success_CancelsInvitationsAndMarksMembersInactive() {
        Invitation pendingInvite = new Invitation();
        pendingInvite.setId(1L);
        pendingInvite.setStatus(InvitationStatus.PENDING);

        Invitation acceptedInvite = new Invitation();
        acceptedInvite.setId(2L);
        acceptedInvite.setStatus(InvitationStatus.ACCEPTED);

        TeamMember activeMember = new TeamMember();
        activeMember.setId(1L);
        activeMember.setIsActive(true);

        when(invitationRepository.findByStartupId(101L)).thenReturn(List.of(pendingInvite, acceptedInvite));
        when(teamMemberRepository.findByStartupIdAndIsActiveTrue(101L)).thenReturn(List.of(activeMember));

        consumer.handleStartupDeletedEvent(testEvent);

        verify(invitationRepository).save(pendingInvite);
        verify(invitationRepository, times(1)).save(any(Invitation.class)); // Only the pending one
        verify(teamMemberRepository).save(activeMember);
    }

    @Test
    void handleStartupDeletedEvent_NoPendingInvitationsOrActiveMembers_DoesNothing() {
        when(invitationRepository.findByStartupId(101L)).thenReturn(Collections.emptyList());
        when(teamMemberRepository.findByStartupIdAndIsActiveTrue(101L)).thenReturn(Collections.emptyList());

        consumer.handleStartupDeletedEvent(testEvent);

        verify(invitationRepository, times(0)).save(any());
        verify(teamMemberRepository, times(0)).save(any());
    }

    @Test
    void handleStartupDeletedEvent_Exception_CaughtAndLogged() {
        when(invitationRepository.findByStartupId(101L)).thenThrow(new RuntimeException("Database error"));

        // Should not throw exception as it's caught inside the method
        consumer.handleStartupDeletedEvent(testEvent);
        
        verify(invitationRepository, atLeastOnce()).findByStartupId(101L);
    }
}
