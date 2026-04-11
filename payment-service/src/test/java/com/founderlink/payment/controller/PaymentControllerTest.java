package com.founderlink.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.payment.dto.request.ConfirmPaymentRequest;
import com.founderlink.payment.dto.request.CreateOrderRequest;
import com.founderlink.payment.dto.response.ConfirmPaymentResponse;
import com.founderlink.payment.dto.response.CreateOrderResponse;
import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.repository.PaymentRepository;
import com.founderlink.payment.serviceImpl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PaymentController.class, properties = {"spring.cloud.config.enabled=false", "spring.cloud.config.import-check.enabled=false"})
@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentServiceImpl paymentService;

    @MockBean
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrder_Success() throws Exception {
        Long userId = 100L;
        Long investmentId = 200L;
        CreateOrderRequest request = new CreateOrderRequest(investmentId);

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setInvestorId(userId);
        payment.setInvestmentId(investmentId);

        CreateOrderResponse response = new CreateOrderResponse("order_123", BigDecimal.valueOf(100), "INR", investmentId);

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
    void confirmPayment_Success() throws Exception {
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
                .andExpect(jsonPath("$.message").value("Payment confirmed successfully"))
                .andExpect(jsonPath("$.data.investmentId").value(1L));
    }

    @Test
    void getPayment_Success() throws Exception {
        Long userId = 100L;
        Long paymentId = 1L;

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setInvestorId(userId);

        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setId(paymentId);
        responseDto.setInvestmentId(200L);
        responseDto.setInvestorId(userId);
        responseDto.setStartupId(300L);
        responseDto.setExternalPaymentId("order_123");
        responseDto.setAmount(BigDecimal.valueOf(500));
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
    void createOrder_AccessDeniedWrongRole() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(200L);

        mockMvc.perform(post("/payments/create-order")
                .header("X-User-Id", 100L)
                .header("X-User-Role", "ROLE_STARTUP")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }
}
