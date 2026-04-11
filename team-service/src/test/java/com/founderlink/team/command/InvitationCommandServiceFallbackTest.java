package com.founderlink.team.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.founderlink.team.dto.request.InvitationRequestDto;
import com.founderlink.team.exception.DuplicateInvitationException;
import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.exception.StartupNotFoundException;
import com.founderlink.team.exception.StartupServiceUnavailableException;
import com.founderlink.team.exception.UnauthorizedAccessException;
import com.founderlink.team.mapper.InvitationMapper;
import com.founderlink.team.repository.InvitationRepository;
import com.founderlink.team.events.TeamEventPublisher;
import com.founderlink.team.client.StartupServiceClient;

class InvitationCommandServiceFallbackTest {

    @Mock private InvitationRepository invitationRepository;
    @Mock private InvitationMapper invitationMapper;
    @Mock private TeamEventPublisher eventPublisher;
    @Mock private StartupServiceClient startupServiceClient;

    @InjectMocks
    private InvitationCommandService invitationCommandService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendInvitationFallback_RethrowsKnownExceptions() {
        Throwable startupNotFound = new StartupNotFoundException("Not found");
        assertThatThrownBy(() -> invitationCommandService.sendInvitationFallback(5L, new InvitationRequestDto(), startupNotFound))
                .isSameAs(startupNotFound);

        Throwable forbidden = new ForbiddenAccessException("Forbidden");
        assertThatThrownBy(() -> invitationCommandService.sendInvitationFallback(5L, new InvitationRequestDto(), forbidden))
                .isSameAs(forbidden);

        Throwable duplicate = new DuplicateInvitationException("Duplicate");
        assertThatThrownBy(() -> invitationCommandService.sendInvitationFallback(5L, new InvitationRequestDto(), duplicate))
                .isSameAs(duplicate);

        Throwable unauthorized = new UnauthorizedAccessException("Unauthorized");
        assertThatThrownBy(() -> invitationCommandService.sendInvitationFallback(5L, new InvitationRequestDto(), unauthorized))
                .isSameAs(unauthorized);
    }

    @Test
    void sendInvitationFallback_ThrowsServiceUnavailableForGenericExceptions() {
        Throwable generic = new RuntimeException("Something else");
        assertThatThrownBy(() -> invitationCommandService.sendInvitationFallback(5L, new InvitationRequestDto(), generic))
                .isInstanceOf(StartupServiceUnavailableException.class)
                .hasMessageContaining("Startup service is temporarily unavailable");
    }
}
