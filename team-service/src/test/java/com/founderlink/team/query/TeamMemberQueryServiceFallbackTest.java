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
import com.founderlink.team.mapper.TeamMemberMapper;
import com.founderlink.team.repository.TeamMemberRepository;
import com.founderlink.team.client.StartupServiceClient;

class TeamMemberQueryServiceFallbackTest {

    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamMemberMapper teamMemberMapper;
    @Mock private StartupServiceClient startupServiceClient;

    @InjectMocks
    private TeamMemberQueryService teamMemberQueryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getTeamByStartupIdFallback_RethrowsKnownExceptions() {
        Throwable startupNotFound = new StartupNotFoundException("Not found");
        assertThatThrownBy(() -> teamMemberQueryService.getTeamByStartupIdFallback(101L, 5L, "ROLE_FOUNDER", startupNotFound))
                .isSameAs(startupNotFound);

        Throwable forbidden = new ForbiddenAccessException("Forbidden");
        assertThatThrownBy(() -> teamMemberQueryService.getTeamByStartupIdFallback(101L, 5L, "ROLE_FOUNDER", forbidden))
                .isSameAs(forbidden);
    }

    @Test
    void getTeamByStartupIdFallback_ThrowsServiceUnavailableForGenericExceptions() {
        Throwable generic = new RuntimeException("Something else");
        assertThatThrownBy(() -> teamMemberQueryService.getTeamByStartupIdFallback(101L, 5L, "ROLE_FOUNDER", generic))
                .isInstanceOf(StartupServiceUnavailableException.class)
                .hasMessageContaining("Startup service is temporarily unavailable");
    }
}
