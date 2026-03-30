package com.founderlink.payment.service;

import com.founderlink.payment.client.WalletServiceClient;
import com.founderlink.payment.dto.external.WalletDepositRequestDto;
import com.founderlink.payment.dto.response.CreateOrderResponse;
import com.founderlink.payment.dto.response.ConfirmPaymentResponse;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.event.PaymentCompletedEvent;
import com.founderlink.payment.event.PaymentFailedEvent;
import com.founderlink.payment.event.PaymentResultEventPublisher;
import com.founderlink.payment.exception.PaymentGatewayException;
import com.founderlink.payment.exception.PaymentNotFoundException;
import com.founderlink.payment.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final PaymentResultEventPublisher paymentResultEventPublisher;
    private final WalletServiceClient walletServiceClient;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Transactional
    public CreateOrderResponse createOrder(Long investmentId) {

        log.info("Creating Razorpay order for investment: {}", investmentId);

        // 🔒 Fetch payment (source of truth)
        Payment payment = paymentRepository.findByInvestmentId(investmentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not initialized for investment: " + investmentId));

        // ❌ Prevent duplicate successful payment
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new PaymentGatewayException(
                    "Payment already completed for investment: " + investmentId);
        }

        // 🔁 Reuse existing order (idempotency)
        if (payment.getStatus() == PaymentStatus.INITIATED
                && payment.getRazorpayOrderId() != null) {

            log.info("Returning existing Razorpay order for investment: {}", investmentId);

            return new CreateOrderResponse(
                    payment.getRazorpayOrderId(),
                    payment.getAmount(),
                    "INR",
                    investmentId);
        }

        try {
            // 💰 Convert amount safely (₹ → paise)
            int amountInPaise = payment.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .intValueExact();

            // 🧾 Create Razorpay order
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "inv_" + investmentId);

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");

            log.info("Razorpay order created: {}", orderId);

            // 🧠 Update ONLY order-related fields (do NOT overwrite core data)
            payment.setRazorpayOrderId(orderId);
            payment.setStatus(PaymentStatus.INITIATED);
            payment.setUpdatedAt(LocalDateTime.now());

            paymentRepository.save(payment);

            return new CreateOrderResponse(
                    orderId,
                    payment.getAmount(),
                    "INR",
                    investmentId);

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage(), e);
            throw new PaymentGatewayException(
                    "Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }

    @Transactional(noRollbackFor = PaymentGatewayException.class)
    public ConfirmPaymentResponse confirmPayment(String orderId, String paymentId, String signature) {

        log.info("Confirming payment - orderId: {}, paymentId: {}", orderId, paymentId);

        // 🔍 Fetch payment (source of truth)
        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for order: " + orderId));

        // 🔁 Idempotency: already processed
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already confirmed for orderId: {}", orderId);
            return new ConfirmPaymentResponse("SUCCESS", payment.getInvestmentId());
        }

        // ❌ Prevent invalid state transition
        if (payment.getStatus() == PaymentStatus.FAILED) {
            throw new PaymentGatewayException("Cannot confirm a failed payment");
        }

        // 🔐 Verify signature (Razorpay)
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(attributes, keySecret);

            if (!isValid) {
                log.error("Invalid Razorpay signature for orderId: {}", orderId);
                failPayment(payment, "Invalid payment signature");
                throw new PaymentGatewayException("Invalid payment signature");
            }

        } catch (RazorpayException e) {
            log.error("Signature verification failed: {}", e.getMessage(), e);
            failPayment(payment, "Signature verification failed: " + e.getMessage());
            throw new PaymentGatewayException("Signature verification failed", e);
        }

        // ✅ Update payment (single source of truth)
        payment.setRazorpayPaymentId(paymentId);
        payment.setRazorpaySignature(signature);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        log.info("Payment confirmed successfully for investment: {}", payment.getInvestmentId());

        // 💰 Credit wallet (synchronous call)
        try {
            walletServiceClient.createWallet(payment.getStartupId());
            
            WalletDepositRequestDto depositRequest = new WalletDepositRequestDto();
            depositRequest.setReferenceId(payment.getInvestmentId());
            depositRequest.setStartupId(payment.getStartupId());
            depositRequest.setAmount(payment.getAmount());
            depositRequest.setSourcePaymentId(payment.getId());
            depositRequest.setIdempotencyKey("wallet-deposit-" + payment.getInvestmentId());
            
            walletServiceClient.depositFunds(depositRequest);

            payment.setWalletCredited(true);
            paymentRepository.save(payment);

            log.info("Wallet credited successfully for startup: {}, amount: {}", 
                    payment.getStartupId(), payment.getAmount());
        } catch (Exception e) {
            log.error("Failed to credit wallet for startup: {}, error: {}. Will be retried by scheduled job.", 
                    payment.getStartupId(), e.getMessage(), e);
        }

        // 📢 Publish event AFTER state update
        paymentResultEventPublisher.publishPaymentCompleted(
                new PaymentCompletedEvent(
                        payment.getInvestmentId(),
                        payment.getId(),
                        payment.getInvestorId(),
                        payment.getFounderId(),
                        payment.getStartupId(),
                        payment.getAmount()
                ));

        return new ConfirmPaymentResponse("SUCCESS", payment.getInvestmentId());
    }

    private void failPayment(Payment payment, String reason) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        paymentResultEventPublisher.publishPaymentFailed(
                new PaymentFailedEvent(
                        payment.getInvestmentId(),
                        payment.getId(),
                        payment.getInvestorId(),
                        payment.getFounderId(),
                        payment.getStartupId(),
                        payment.getAmount(),
                        reason
                ));

        log.info("Payment marked FAILED for investment: {}, reason: {}",
                payment.getInvestmentId(), reason);
    }
}
