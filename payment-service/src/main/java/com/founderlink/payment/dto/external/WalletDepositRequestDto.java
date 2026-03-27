package com.founderlink.payment.dto.external;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for wallet-service communication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletDepositRequestDto {

    @NotNull(message = "referenceId is required")
    private Long referenceId;

    @NotNull(message = "startupId is required")
    private Long startupId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "sourcePaymentId is required")
    private Long sourcePaymentId;

    @NotNull(message = "idempotencyKey is required")
    private String idempotencyKey;
}
