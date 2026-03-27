package com.founderlink.payment.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event published by investment-service when investment is approved.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentApprovedEvent {

    private Long investmentId;

    private Long investorId;

    private Long founderId;

    private Long startupId;

    private BigDecimal amount;
}
