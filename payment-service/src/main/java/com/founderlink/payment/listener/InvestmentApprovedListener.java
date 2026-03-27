package com.founderlink.payment.listener;

import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.event.InvestmentApprovedEvent;
import com.founderlink.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Listener for InvestmentApprovedEvent.
 * Creates Payment entity when investment is approved by founder.
 * This initializes the payment record so that user can later trigger Razorpay checkout.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InvestmentApprovedListener {

    private final PaymentRepository paymentRepository;

    @RabbitListener(queues = "investment.approved.queue")
    @Transactional
    public void handleInvestmentApproved(InvestmentApprovedEvent event) {
        log.info("Received InvestmentApprovedEvent - investmentId: {}, amount: {}", 
                event.getInvestmentId(), event.getAmount());

        // Idempotency check: prevent duplicate Payment creation
        if (paymentRepository.findByInvestmentId(event.getInvestmentId()).isPresent()) {
            log.warn("Payment already exists for investmentId: {} - skipping creation", 
                    event.getInvestmentId());
            return;
        }

        // Create Payment entity
        Payment payment = new Payment();
        payment.setInvestmentId(event.getInvestmentId());
        payment.setInvestorId(event.getInvestorId());
        payment.setStartupId(event.getStartupId());
        payment.setFounderId(event.getFounderId());
        payment.setAmount(event.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey("payment-init-" + event.getInvestmentId());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        log.info("Payment entity created successfully - investmentId: {}, paymentId: {}", 
                event.getInvestmentId(), payment.getId());
    }
}
