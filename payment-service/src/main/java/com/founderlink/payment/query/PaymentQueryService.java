package com.founderlink.payment.query;

import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.exception.PaymentNotFoundException;
import com.founderlink.payment.mapper.PaymentMapper;
import com.founderlink.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Cacheable(value = "paymentById", key = "#paymentId")
    public PaymentResponseDto getPaymentById(Long paymentId) {
        log.info("QUERY - getPaymentById: {} (cache miss, hitting DB)", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));
        return paymentMapper.toResponseDto(payment);
    }

    @Cacheable(value = "paymentByInvestment", key = "#investmentId")
    public PaymentResponseDto getPaymentByInvestmentId(Long investmentId) {
        log.info("QUERY - getPaymentByInvestmentId: {} (cache miss, hitting DB)", investmentId);
        Payment payment = paymentRepository.findByInvestmentId(investmentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for investment ID: " + investmentId));
        return paymentMapper.toResponseDto(payment);
    }

    @Cacheable(value = "paymentById", key = "#paymentId + '_status'")
    public PaymentStatus getPaymentStatus(Long paymentId) {
        log.info("QUERY - getPaymentStatus: {} (cache miss, hitting DB)", paymentId);
        return paymentRepository.findById(paymentId)
                .map(Payment::getStatus)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));
    }
}
