package com.founderlink.payment.dto.external;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for wallet-service response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponseDto {

    private Long id;

    private Long startupId;

    private BigDecimal balance;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
