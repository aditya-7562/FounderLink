package com.founderlink.team.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.founderlink.team.dto.response.TeamMemberResponseDto;
import com.founderlink.team.entity.TeamMember;
import com.founderlink.team.entity.TeamRole;

class TeamMemberMapperTest {

    private TeamMemberMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TeamMemberMapper();
    }

    @Test
    void toResponseDto_mapsAllFieldsCorrectly() {
        TeamMember entity = new TeamMember();
        entity.setId(1L);
        entity.setStartupId(101L);
        entity.setUserId(300L);
        entity.setRole(TeamRole.CTO);
        entity.setIsActive(true);
        LocalDateTime now = LocalDateTime.now();
        entity.setJoinedAt(now);
        entity.setLeftAt(null);

        TeamMemberResponseDto responseDto = mapper.toResponseDto(entity);

        assertThat(responseDto.getId()).isEqualTo(1L);
        assertThat(responseDto.getStartupId()).isEqualTo(101L);
        assertThat(responseDto.getUserId()).isEqualTo(300L);
        assertThat(responseDto.getRole()).isEqualTo(TeamRole.CTO);
        assertThat(responseDto.getIsActive()).isTrue();
        assertThat(responseDto.getJoinedAt()).isEqualTo(now);
        assertThat(responseDto.getLeftAt()).isNull();
    }
}
