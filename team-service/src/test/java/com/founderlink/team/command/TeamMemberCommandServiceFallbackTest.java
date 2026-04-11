package com.founderlink.team.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.exception.StartupNotFoundException;
import com.founderlink.team.exception.StartupServiceUnavailableException;
import com.founderlink.team.exception.TeamMemberNotFoundException;
import com.founderlink.team.exception.UnauthorizedAccessException;
import com.founderlink.team.mapper.TeamMemberMapper;
import com.founderlink.team.repository.InvitationRepository;
import com.founderlink.team.repository.TeamMemberRepository;
import com.founderlink.team.events.TeamEventPublisher;
import com.founderlink.team.client.StartupServiceClient;

class TeamMemberCommandServiceFallbackTest {

    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private InvitationRepository invitationRepository;
    @Mock private TeamMemberMapper teamMemberMapper;
    @Mock private StartupServiceClient startupServiceClient;
    @Mock private TeamEventPublisher teamEventPublisher;

    @InjectMocks
    private TeamMemberCommandService teamMemberCommandService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void removeTeamMemberFallback_RethrowsKnownExceptions() {
        Throwable memberNotFound = new TeamMemberNotFoundException("Not found");
        assertThatThrownBy(() -> teamMemberCommandService.removeTeamMemberFallback(1L, 5L, memberNotFound))
                .isSameAs(memberNotFound);

        Throwable forbidden = new ForbiddenAccessException("Forbidden");
        assertThatThrownBy(() -> teamMemberCommandService.removeTeamMemberFallback(1L, 5L, forbidden))
                .isSameAs(forbidden);

        Throwable startupNotFound = new StartupNotFoundException("Startup not found");
        assertThatThrownBy(() -> teamMemberCommandService.removeTeamMemberFallback(1L, 5L, startupNotFound))
                .isSameAs(startupNotFound);

        Throwable unauthorized = new UnauthorizedAccessException("Unauthorized");
        assertThatThrownBy(() -> teamMemberCommandService.removeTeamMemberFallback(1L, 5L, unauthorized))
                .isSameAs(unauthorized);
    }

    @Test
    void removeTeamMemberFallback_ThrowsServiceUnavailableForGenericExceptions() {
        Throwable generic = new RuntimeException("Something else");
        assertThatThrownBy(() -> teamMemberCommandService.removeTeamMemberFallback(1L, 5L, generic))
                .isInstanceOf(StartupServiceUnavailableException.class)
                .hasMessageContaining("Startup service is temporarily unavailable");
    }
}
