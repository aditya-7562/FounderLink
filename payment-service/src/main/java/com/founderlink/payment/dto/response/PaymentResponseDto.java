package com.founderlink.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.founderlink.payment.entity.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private Long id;

    private Long investmentId;

    private Long investorId;

    private Long startupId;

    private Long founderId;

    private BigDecimal amount;

    private PaymentStatus status;

    private String externalPaymentId;

    private String failureReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
