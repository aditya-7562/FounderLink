package com.founderlink.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.payment.client.WalletServiceClient;
import com.founderlink.payment.dto.request.ConfirmPaymentRequest;
import com.founderlink.payment.dto.request.CreateOrderRequest;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.repository.PaymentRepository;
import com.razorpay.Order;
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

@SpringBootTest
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

    @Test
    void createOrderE2E() throws Exception {
        Long userId = 100L;
        Long investmentId = 200L;
        CreateOrderRequest request = new CreateOrderRequest(investmentId);

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setInvestorId(userId);
        payment.setInvestmentId(investmentId);
        payment.setAmount(BigDecimal.valueOf(500));
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setRazorpayOrderId("order_existing");

        when(paymentRepository.findByInvestmentId(investmentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/payments/create-order")
                .header("X-User-Id", userId)
                .header("X-User-Role", "ROLE_INVESTOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Razorpay order created successfully"))
                .andExpect(jsonPath("$.data.orderId").value("order_existing"));
    }

    @Test
    void getPaymentE2E() throws Exception {
        Long userId = 100L;
        Long paymentId = 1L;

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setInvestorId(userId);
        payment.setAmount(BigDecimal.valueOf(1000));
        payment.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        mockMvc.perform(get("/payments/{paymentId}", paymentId)
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(paymentId))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }
}
