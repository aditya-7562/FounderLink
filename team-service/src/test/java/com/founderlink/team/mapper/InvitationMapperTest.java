package com.founderlink.team.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.founderlink.team.dto.request.InvitationRequestDto;
import com.founderlink.team.dto.response.InvitationResponseDto;
import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.entity.TeamRole;

class InvitationMapperTest {

    private InvitationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new InvitationMapper();
    }

    @Test
    void toEntity_mapsAllFieldsCorrectly() {
        InvitationRequestDto dto = new InvitationRequestDto();
        dto.setStartupId(101L);
        dto.setInvitedUserId(300L);
        dto.setRole(TeamRole.CTO);

        Invitation entity = mapper.toEntity(dto, 5L);

        assertThat(entity.getStartupId()).isEqualTo(101L);
        assertThat(entity.getFounderId()).isEqualTo(5L);
        assertThat(entity.getInvitedUserId()).isEqualTo(300L);
        assertThat(entity.getRole()).isEqualTo(TeamRole.CTO);
    }

    @Test
    void toResponseDto_mapsAllFieldsCorrectly() {
        Invitation entity = new Invitation();
        entity.setId(1L);
        entity.setStartupId(101L);
        entity.setFounderId(5L);
        entity.setInvitedUserId(300L);
        entity.setRole(TeamRole.CTO);
        entity.setStatus(InvitationStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        InvitationResponseDto responseDto = mapper.toResponseDto(entity);

        assertThat(responseDto.getId()).isEqualTo(1L);
        assertThat(responseDto.getStartupId()).isEqualTo(101L);
        assertThat(responseDto.getFounderId()).isEqualTo(5L);
        assertThat(responseDto.getInvitedUserId()).isEqualTo(300L);
        assertThat(responseDto.getRole()).isEqualTo(TeamRole.CTO);
        assertThat(responseDto.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(responseDto.getCreatedAt()).isEqualTo(now);
        assertThat(responseDto.getUpdatedAt()).isEqualTo(now);
    }
}
