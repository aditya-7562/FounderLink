package com.founderlink.payment.serviceImpl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.payment.command.PaymentCommandService;
import com.founderlink.payment.dto.response.ConfirmPaymentResponse;
import com.founderlink.payment.dto.response.CreateOrderResponse;
import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.query.PaymentQueryService;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentCommandService commandService;

    @Mock
    private PaymentQueryService queryService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void getPaymentById_Delegates() {
        paymentService.getPaymentById(1L);
        verify(queryService).getPaymentById(1L);
    }

    @Test
    void getPaymentByInvestmentId_Delegates() {
        paymentService.getPaymentByInvestmentId(200L);
        verify(queryService).getPaymentByInvestmentId(200L);
    }

    @Test
    void getPaymentStatus_Delegates() {
        paymentService.getPaymentStatus(1L);
        verify(queryService).getPaymentStatus(1L);
    }

    @Test
    void createOrder_Delegates() {
        paymentService.createOrder(100L);
        verify(commandService).createOrder(100L);
    }

    @Test
    void confirmPayment_Delegates() {
        paymentService.confirmPayment("o1", "p1", "s1");
        verify(commandService).confirmPayment("o1", "p1", "s1");
    }
}
