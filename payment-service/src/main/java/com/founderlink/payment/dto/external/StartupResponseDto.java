package com.founderlink.payment.dto.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for startup-service response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartupResponseDto {

    private Long id;

    private String name;

    private Long founderId;

    private String industry;
}
