package com.founderlink.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
