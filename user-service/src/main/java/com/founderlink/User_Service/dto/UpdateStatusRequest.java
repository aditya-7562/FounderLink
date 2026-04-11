package com.founderlink.User_Service.dto;

import com.founderlink.User_Service.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private UserStatus status;

    private String reason;
}
