package com.founderlink.payment.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event published by investment-service when investment is created.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentCreatedEvent {

    private Long investmentId;

    private Long investorId;

    private Long founderId;

    private Long startupId;

    private BigDecimal amount;

    private Long investmentRoundId;
}
