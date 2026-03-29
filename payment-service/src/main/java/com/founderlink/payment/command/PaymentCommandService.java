package com.founderlink.payment.command;

import com.founderlink.payment.dto.response.ConfirmPaymentResponse;
import com.founderlink.payment.dto.response.CreateOrderResponse;
import com.founderlink.payment.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentCommandService {

    private final RazorpayService razorpayService;

    @Caching(evict = {
        @CacheEvict(value = "paymentByInvestment", key = "#investmentId")
    })
    public CreateOrderResponse createOrder(Long investmentId) {
        log.info("COMMAND - createOrder: investmentId={}", investmentId);
        return razorpayService.createOrder(investmentId);
    }

    @Caching(evict = {
        @CacheEvict(value = "paymentById", allEntries = true),
        @CacheEvict(value = "paymentByInvestment", allEntries = true)
    })
    public ConfirmPaymentResponse confirmPayment(String razorpayOrderId, String razorpayPaymentId,
                                                  String razorpaySignature) {
        log.info("COMMAND - confirmPayment: orderId={}", razorpayOrderId);
        return razorpayService.confirmPayment(razorpayOrderId, razorpayPaymentId, razorpaySignature);
    }
}
