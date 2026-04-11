package com.founderlink.team.query;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.exception.StartupNotFoundException;
import com.founderlink.team.exception.StartupServiceUnavailableException;
import com.founderlink.team.mapper.InvitationMapper;
import com.founderlink.team.repository.InvitationRepository;
import com.founderlink.team.client.StartupServiceClient;

class InvitationQueryServiceFallbackTest {

    @Mock private InvitationRepository invitationRepository;
    @Mock private InvitationMapper invitationMapper;
    @Mock private StartupServiceClient startupServiceClient;

    @InjectMocks
    private InvitationQueryService invitationQueryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getInvitationsByStartupIdFallback_RethrowsKnownExceptions() {
        Throwable startupNotFound = new StartupNotFoundException("Not found");
        assertThatThrownBy(() -> invitationQueryService.getInvitationsByStartupIdFallback(101L, 5L, startupNotFound))
                .isSameAs(startupNotFound);

        Throwable forbidden = new ForbiddenAccessException("Forbidden");
        assertThatThrownBy(() -> invitationQueryService.getInvitationsByStartupIdFallback(101L, 5L, forbidden))
                .isSameAs(forbidden);
    }

    @Test
    void getInvitationsByStartupIdFallback_ThrowsServiceUnavailableForGenericExceptions() {
        Throwable generic = new RuntimeException("Something else");
        assertThatThrownBy(() -> invitationQueryService.getInvitationsByStartupIdFallback(101L, 5L, generic))
                .isInstanceOf(StartupServiceUnavailableException.class)
                .hasMessageContaining("Startup service is temporarily unavailable");
    }
}
