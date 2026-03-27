package com.founderlink.investment.events;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a founder rejects an investment.
 * Triggers payment release in payment-service (Saga compensation).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentRejectedEvent {

    private Long investmentId;
    private Long investorId;
    private Long founderId;
    private Long startupId;
    private BigDecimal amount;
    private String rejectionReason;
}
