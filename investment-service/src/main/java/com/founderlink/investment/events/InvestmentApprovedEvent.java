package com.founderlink.investment.events;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a founder approves an investment.
 * Triggers payment capture in payment-service (Saga orchestration).
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
