package com.founderlink.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.payment.client.WalletServiceClient;
import com.founderlink.payment.dto.request.ConfirmPaymentRequest;
import com.founderlink.payment.dto.request.CreateOrderRequest;
import com.founderlink.payment.dto.response.ConfirmPaymentResponse;
import com.founderlink.payment.dto.response.CreateOrderResponse;
import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.repository.PaymentRepository;
import com.founderlink.payment.serviceImpl.PaymentServiceImpl;
import com.founderlink.payment.event.PaymentResultEventPublisher;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "spring.cloud.config.import-check.enabled=false"})
@AutoConfigureMockMvc
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private RazorpayClient razorpayClient;

    @MockBean
    private WalletServiceClient walletServiceClient;

    @MockBean
    private PaymentResultEventPublisher paymentResultEventPublisher;

    @MockBean
    private PaymentServiceImpl paymentService;

    @Test
    void createOrderE2E() throws Exception {
        Long userId = 100L;
        Long investmentId = 200L;
        CreateOrderRequest request = new CreateOrderRequest(investmentId);

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setInvestorId(userId);
        payment.setInvestmentId(investmentId);

        CreateOrderResponse response = new CreateOrderResponse("order_123", BigDecimal.valueOf(500), "INR", investmentId);

        when(paymentRepository.findByInvestmentId(investmentId)).thenReturn(Optional.of(payment));
        when(paymentService.createOrder(investmentId)).thenReturn(response);

        mockMvc.perform(post("/payments/create-order")
                .header("X-User-Id", userId)
                .header("X-User-Role", "ROLE_INVESTOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Razorpay order created successfully"))
                .andExpect(jsonPath("$.data.orderId").value("order_123"));
    }

    @Test
    void confirmPaymentE2E() throws Exception {
        Long userId = 100L;
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("order_123", "pay_456", "sign_789");

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setInvestorId(userId);
        payment.setRazorpayOrderId("order_123");

        ConfirmPaymentResponse response = new ConfirmPaymentResponse("SUCCESS", 1L);

        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));
        when(paymentService.confirmPayment("order_123", "pay_456", "sign_789")).thenReturn(response);

        mockMvc.perform(post("/payments/confirm")
                .header("X-User-Id", userId)
                .header("X-User-Role", "ROLE_INVESTOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment confirmed successfully"));
    }

    @Test
    void getPaymentE2E() throws Exception {
        Long userId = 100L;
        Long paymentId = 1L;

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setInvestorId(userId);

        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setId(paymentId);
        responseDto.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentService.getPaymentById(paymentId)).thenReturn(responseDto);

        mockMvc.perform(get("/payments/{paymentId}", paymentId)
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(paymentId));
    }

    @Test
    void getPaymentByInvestmentIdE2E() throws Exception {
        Long userId = 100L;
        Long investmentId = 200L;
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setInvestmentId(investmentId);
        payment.setInvestorId(userId);

        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setId(1L);
        responseDto.setInvestmentId(investmentId);

        when(paymentRepository.findByInvestmentId(investmentId)).thenReturn(Optional.of(payment));
        when(paymentService.getPaymentByInvestmentId(investmentId)).thenReturn(responseDto);

        mockMvc.perform(get("/payments/investment/{investmentId}", investmentId)
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.investmentId").value(investmentId));
    }
}
