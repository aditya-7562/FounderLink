package com.founderlink.payment.mapper;

import org.springframework.stereotype.Component;

import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.entity.Payment;

@Component
public class PaymentMapper {

    public PaymentResponseDto toResponseDto(Payment payment) {
        if (payment == null) {
            return null;
        }

        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setId(payment.getId());
        dto.setInvestmentId(payment.getInvestmentId());
        dto.setInvestorId(payment.getInvestorId());
        dto.setStartupId(payment.getStartupId());
        dto.setFounderId(payment.getFounderId());
        dto.setAmount(payment.getAmount());
        dto.setStatus(payment.getStatus());
        dto.setExternalPaymentId(payment.getExternalPaymentId());
        dto.setFailureReason(payment.getFailureReason());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());

        return dto;
    }
}
