package com.founderlink.team.service.team;
import com.founderlink.team.client.StartupServiceClient;
import com.founderlink.team.dto.response.TeamMemberResponseDto;
import com.founderlink.team.entity.TeamMember;
import com.founderlink.team.entity.TeamRole;
import com.founderlink.team.mapper.TeamMemberMapper;
import com.founderlink.team.repository.InvitationRepository;
import com.founderlink.team.repository.TeamMemberRepository;
import com.founderlink.team.serviceImpl.TeamMemberServiceImpl;
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
class GetMemberHistoryTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private StartupServiceClient startupServiceClient;

    @InjectMocks
    private TeamMemberServiceImpl teamMemberService;

    private TeamMember activeMember;
    private TeamMember inactiveMember;
    private TeamMemberResponseDto activeResponseDto;
    private TeamMemberResponseDto inactiveResponseDto;

    @BeforeEach
    void setUp() {
        // Active membership
        activeMember = new TeamMember();
        activeMember.setId(1L);
        activeMember.setStartupId(101L);
        activeMember.setUserId(202L);
        activeMember.setRole(TeamRole.CTO);
        activeMember.setIsActive(true);
        activeMember.setLeftAt(null);
        activeMember.setJoinedAt(LocalDateTime.now());

        // Inactive membership
        inactiveMember = new TeamMember();
        inactiveMember.setId(2L);
        inactiveMember.setStartupId(202L);
        inactiveMember.setUserId(202L);
        inactiveMember.setRole(TeamRole.CPO);
        inactiveMember.setIsActive(false);
        inactiveMember.setLeftAt(LocalDateTime.now());
        inactiveMember.setJoinedAt(
                LocalDateTime.now().minusMonths(6));

        activeResponseDto = new TeamMemberResponseDto();
        activeResponseDto.setId(1L);
        activeResponseDto.setStartupId(101L);
        activeResponseDto.setUserId(202L);
        activeResponseDto.setRole(TeamRole.CTO);
        activeResponseDto.setIsActive(true);
        activeResponseDto.setLeftAt(null);

        inactiveResponseDto = new TeamMemberResponseDto();
        inactiveResponseDto.setId(2L);
        inactiveResponseDto.setStartupId(202L);
        inactiveResponseDto.setUserId(202L);
        inactiveResponseDto.setRole(TeamRole.CPO);
        inactiveResponseDto.setIsActive(false);
        inactiveResponseDto.setLeftAt(
                LocalDateTime.now());
    }

    // ─────────────────────────────────────────
    // SUCCESS — RETURNS ALL RECORDS
    // ─────────────────────────────────────────
    @Test
    void getMemberHistory_Success() {

        // Arrange
        when(teamMemberRepository
                .findByUserId(202L))
                .thenReturn(List.of(
                        activeMember,
                        inactiveMember));
        when(teamMemberMapper
                .toResponseDto(activeMember))
                .thenReturn(activeResponseDto);
        when(teamMemberMapper
                .toResponseDto(inactiveMember))
                .thenReturn(inactiveResponseDto);

        // Act
        List<TeamMemberResponseDto> result =
                teamMemberService
                        .getMemberHistory(202L);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getIsActive())
                .isTrue();
        assertThat(result.get(1).getIsActive())
                .isFalse();
        assertThat(result.get(1).getLeftAt())
                .isNotNull();
    }

    // ─────────────────────────────────────────
    // EMPTY HISTORY
    // ─────────────────────────────────────────
    @Test
    void getMemberHistory_NoHistory_ReturnsEmpty() {

        // Arrange
        when(teamMemberRepository
                .findByUserId(202L))
                .thenReturn(List.of());

        // Act
        List<TeamMemberResponseDto> result =
                teamMemberService
                        .getMemberHistory(202L);

        // Assert
        assertThat(result).isEmpty();
    }
}