package com.founderlink.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.founderlink.payment.client.WalletServiceClient;
import com.founderlink.payment.dto.external.WalletDepositRequestDto;
import com.founderlink.payment.dto.response.ConfirmPaymentResponse;
import com.founderlink.payment.dto.response.CreateOrderResponse;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.event.PaymentCompletedEvent;
import com.founderlink.payment.event.PaymentResultEventPublisher;
import com.founderlink.payment.exception.PaymentGatewayException;
import com.founderlink.payment.exception.PaymentNotFoundException;
import com.founderlink.payment.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

@ExtendWith(MockitoExtension.class)
class RazorpayServiceTest {

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentResultEventPublisher paymentResultEventPublisher;

    @Mock
    private WalletServiceClient walletServiceClient;

    @InjectMocks
    private RazorpayService razorpayService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(razorpayService, "keySecret", "test_secret");
    }

    @Test
    void createOrder_Success() throws Exception {
        Long investmentId = 1L;
        Payment payment = new Payment();
        payment.setId(100L);
        payment.setAmount(new BigDecimal("500.00"));
        payment.setInvestmentId(investmentId);
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findByInvestmentId(investmentId)).thenReturn(Optional.of(payment));

        OrderClient orderClient = mock(OrderClient.class);
        ReflectionTestUtils.setField(razorpayClient, "orders", orderClient);

        Order order = mock(Order.class);
        when(order.get("id")).thenReturn("order_123");
        when(orderClient.create(any(JSONObject.class))).thenReturn(order);

        CreateOrderResponse response = razorpayService.createOrder(investmentId);

        assertThat(response.getOrderId()).isEqualTo("order_123");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        verify(paymentRepository).save(payment);
    }

    @Test
    void createOrder_AlreadyCompleted_ThrowsException() {
        Long investmentId = 1L;
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findByInvestmentId(investmentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> razorpayService.createOrder(investmentId))
                .isInstanceOf(PaymentGatewayException.class);
    }

    @Test
    void createOrder_Idempotency_ReturnsExisting() {
        Long investmentId = 1L;
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setRazorpayOrderId("order_existing");
        payment.setAmount(new BigDecimal("500.00"));
        when(paymentRepository.findByInvestmentId(investmentId)).thenReturn(Optional.of(payment));

        CreateOrderResponse response = razorpayService.createOrder(investmentId);

        assertThat(response.getOrderId()).isEqualTo("order_existing");
        verifyNoInteractions(razorpayClient);
    }

    @Test
    void confirmPayment_Success_WithWalletCredit() throws Exception {
        Payment payment = new Payment();
        payment.setId(100L);
        payment.setInvestmentId(1L);
        payment.setStartupId(200L);
        payment.setAmount(new BigDecimal("500.00"));
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setRazorpayOrderId("order_123");

        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                       .thenReturn(true);

            ConfirmPaymentResponse response = razorpayService.confirmPayment("order_123", "pay_123", "sig_123");

            assertThat(response.getStatus()).isEqualTo("SUCCESS");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getRazorpayPaymentId()).isEqualTo("pay_123");
            
            // Verify wallet credit attempt
            verify(walletServiceClient).createWallet(200L);
            verify(walletServiceClient).depositFunds(any(WalletDepositRequestDto.class));
            verify(paymentResultEventPublisher).publishPaymentCompleted(any(PaymentCompletedEvent.class));
        }
    }

    @Test
    void confirmPayment_InvalidSignature_MarksFailed() throws Exception {
        Payment payment = new Payment();
        payment.setRazorpayOrderId("order_123");
        payment.setStatus(PaymentStatus.INITIATED);
        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                       .thenReturn(false);

            assertThatThrownBy(() -> razorpayService.confirmPayment("order_123", "pay_123", "sig_123"))
                    .isInstanceOf(PaymentGatewayException.class)
                    .hasMessageContaining("Invalid payment signature");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailureReason()).isEqualTo("Invalid payment signature");
        }
    }

    @Test
    void confirmPayment_RazorpayException_MarksFailed() throws Exception {
        Payment payment = new Payment();
        payment.setRazorpayOrderId("order_123");
        payment.setStatus(PaymentStatus.INITIATED);
        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                       .thenThrow(new RazorpayException("Signature error"));

            assertThatThrownBy(() -> razorpayService.confirmPayment("order_123", "pay_123", "sig_123"))
                    .isInstanceOf(PaymentGatewayException.class)
                    .hasMessageContaining("Signature verification failed");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }
    }
    
    @Test
    void confirmPayment_WalletCreditFailure_DoesNotThrow() throws Exception {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.INITIATED);
        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                       .thenReturn(true);

            doThrow(new RuntimeException("Wallet error")).when(walletServiceClient).createWallet(any());

            ConfirmPaymentResponse response = razorpayService.confirmPayment("order_123", "pay_123", "sig_123");

            assertThat(response.getStatus()).isEqualTo("SUCCESS");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.isWalletCredited()).isFalse(); // Should stay false but payment is success
        }
    }

    @Test
    void confirmPayment_AlreadySuccess_Idempotent() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setInvestmentId(1L);
        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));

        ConfirmPaymentResponse response = razorpayService.confirmPayment("order_123", "pay_123", "sig_123");

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void confirmPayment_NotFound_ThrowsException() {
        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> razorpayService.confirmPayment("order_123", "pay_123", "sig_123"))
                .isInstanceOf(PaymentNotFoundException.class);
    }
}
