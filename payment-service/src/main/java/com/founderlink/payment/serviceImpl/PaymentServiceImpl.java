package com.founderlink.payment.serviceImpl;

import com.founderlink.payment.command.PaymentCommandService;
import com.founderlink.payment.dto.response.ConfirmPaymentResponse;
import com.founderlink.payment.dto.response.CreateOrderResponse;
import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.query.PaymentQueryService;
import com.founderlink.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentCommandService commandService;
    private final PaymentQueryService queryService;

    @Override
    public PaymentResponseDto getPaymentById(Long paymentId) {
        return queryService.getPaymentById(paymentId);
    }

    @Override
    public PaymentResponseDto getPaymentByInvestmentId(Long investmentId) {
        return queryService.getPaymentByInvestmentId(investmentId);
    }

    @Override
    public PaymentStatus getPaymentStatus(Long paymentId) {
        return queryService.getPaymentStatus(paymentId);
    }

    public CreateOrderResponse createOrder(Long investmentId) {
        return commandService.createOrder(investmentId);
    }

    public ConfirmPaymentResponse confirmPayment(String razorpayOrderId, String razorpayPaymentId,
                                                  String razorpaySignature) {
        return commandService.confirmPayment(razorpayOrderId, razorpayPaymentId, razorpaySignature);
    }
}
