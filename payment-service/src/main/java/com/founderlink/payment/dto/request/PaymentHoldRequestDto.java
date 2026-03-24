package com.founderlink.payment.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHoldRequestDto {

    @NotNull(message = "investmentId is required")
    private Long investmentId;

    @NotNull(message = "investorId is required")
    private Long investorId;

    @NotNull(message = "startupId is required")
    private Long startupId;

    @NotNull(message = "founderId is required")
    private Long founderId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "idempotencyKey is required")
    private String idempotencyKey;
}
