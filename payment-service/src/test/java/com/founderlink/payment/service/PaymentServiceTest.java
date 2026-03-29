package com.founderlink.payment.service;

import com.founderlink.payment.command.PaymentCommandService;
import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.exception.PaymentNotFoundException;
import com.founderlink.payment.mapper.PaymentMapper;
import com.founderlink.payment.query.PaymentQueryService;
import com.founderlink.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private RazorpayService razorpayService;

    @InjectMocks
    private PaymentQueryService paymentQueryService;

    @InjectMocks
    private PaymentCommandService paymentCommandService;

    @Test
    void getPaymentById_Success() {
        Long paymentId = 1L;
        Payment payment = new Payment();
        payment.setId(paymentId);

        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setId(paymentId);
        responseDto.setInvestmentId(200L);
        responseDto.setInvestorId(100L);
        responseDto.setStartupId(300L);
        responseDto.setExternalPaymentId("order_123");
        responseDto.setAmount(BigDecimal.valueOf(500));
        responseDto.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentMapper.toResponseDto(payment)).thenReturn(responseDto);

        PaymentResponseDto result = paymentQueryService.getPaymentById(paymentId);

        assertNotNull(result);
        assertEquals(paymentId, result.getId());
    }

    @Test
    void getPaymentById_NotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentQueryService.getPaymentById(1L));
    }

    @Test
    void getPaymentByInvestmentId_Success() {
        Long investmentId = 200L;
        Payment payment = new Payment();
        payment.setInvestmentId(investmentId);

        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setId(1L);
        responseDto.setInvestmentId(investmentId);
        responseDto.setInvestorId(100L);
        responseDto.setStartupId(300L);
        responseDto.setExternalPaymentId("order_123");
        responseDto.setAmount(BigDecimal.valueOf(500));
        responseDto.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findByInvestmentId(investmentId)).thenReturn(Optional.of(payment));
        when(paymentMapper.toResponseDto(payment)).thenReturn(responseDto);

        PaymentResponseDto result = paymentQueryService.getPaymentByInvestmentId(investmentId);

        assertNotNull(result);
        assertEquals(investmentId, result.getInvestmentId());
    }

    @Test
    void getPaymentStatus_Success() {
        Long paymentId = 1L;
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentStatus status = paymentQueryService.getPaymentStatus(paymentId);

        assertEquals(PaymentStatus.SUCCESS, status);
    }
}
