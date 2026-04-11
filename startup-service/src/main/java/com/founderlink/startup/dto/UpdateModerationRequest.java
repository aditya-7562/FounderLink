package com.founderlink.startup.dto;

import com.founderlink.startup.entity.ModerationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateModerationRequest {

    @NotNull(message = "Moderation status is required")
    private ModerationStatus status;

    private String reason;
}
