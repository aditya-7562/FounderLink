package com.founderlink.investment.events;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentCreatedEvent {

    private Long startupId;
    private Long investorId;
    private Long founderId;
    private BigDecimal amount;
}