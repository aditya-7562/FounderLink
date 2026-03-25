package com.founderlink.team.service.team;

import com.founderlink.team.dto.response.TeamMemberResponseDto;
import com.founderlink.team.entity.TeamMember;
import com.founderlink.team.entity.TeamRole;
import com.founderlink.team.mapper.TeamMemberMapper;
import com.founderlink.team.query.TeamMemberQueryService;
import com.founderlink.team.repository.TeamMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetActiveMemberRolesTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @InjectMocks
    private TeamMemberQueryService teamMemberService;

    private TeamMember activeMember;
    private TeamMemberResponseDto activeResponseDto;

    @BeforeEach
    void setUp() {
        activeMember = new TeamMember();
        activeMember.setId(1L);
        activeMember.setStartupId(101L);
        activeMember.setUserId(202L);
        activeMember.setRole(TeamRole.CTO);
        activeMember.setIsActive(true);
        activeMember.setLeftAt(null);
        activeMember.setJoinedAt(LocalDateTime.now());

        activeResponseDto = new TeamMemberResponseDto();
        activeResponseDto.setId(1L);
        activeResponseDto.setStartupId(101L);
        activeResponseDto.setUserId(202L);
        activeResponseDto.setRole(TeamRole.CTO);
        activeResponseDto.setIsActive(true);
        activeResponseDto.setLeftAt(null);
    }

    // ─────────────────────────────────────────
    // SUCCESS
    // ─────────────────────────────────────────
    @Test
    void getActiveMemberRoles_Success() {

        // Arrange
        when(teamMemberRepository
                .findByUserIdAndIsActiveTrue(202L))
                .thenReturn(List.of(activeMember));
        when(teamMemberMapper
                .toResponseDto(activeMember))
                .thenReturn(activeResponseDto);

        // Act
        List<TeamMemberResponseDto> result =
                teamMemberService
                        .getActiveMemberRoles(202L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive())
                .isTrue();
        assertThat(result.get(0).getLeftAt())
                .isNull();
    }

    // ─────────────────────────────────────────
    // NO ACTIVE ROLES
    // ─────────────────────────────────────────
    @Test
    void getActiveMemberRoles_NoActiveRoles_ReturnsEmpty() {

        // Arrange
        when(teamMemberRepository
                .findByUserIdAndIsActiveTrue(202L))
                .thenReturn(List.of());

        // Act
        List<TeamMemberResponseDto> result =
                teamMemberService
                        .getActiveMemberRoles(202L);

        // Assert
        assertThat(result).isEmpty();
    }
}