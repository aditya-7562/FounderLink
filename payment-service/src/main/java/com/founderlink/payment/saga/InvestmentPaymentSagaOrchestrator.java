package com.founderlink.payment.saga;

import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.event.InvestmentRejectedEvent;
import com.founderlink.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@Slf4j
public class InvestmentPaymentSagaOrchestrator {

    private final PaymentRepository paymentRepository;

    public InvestmentPaymentSagaOrchestrator(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Handle investment rejection - mark payment as failed.
     * Note: Payment may not exist if investment was rejected before approval.
     */
    @RabbitListener(queues = "investment.rejected.queue")
    @Transactional
    public void handleInvestmentRejected(InvestmentRejectedEvent event) {
        log.info("Investment Rejected: investmentId={}, reason='{}'",
                event.getInvestmentId(), event.getRejectionReason());

        Optional<Payment> paymentOpt = paymentRepository.findByInvestmentId(event.getInvestmentId());

        // If no payment exists, investment was rejected before approval - nothing to do
        if (paymentOpt.isEmpty()) {
            log.info("No payment found for rejected investment {} - likely rejected before approval", 
                    event.getInvestmentId());
            return;
        }

        Payment payment = paymentOpt.get();

        // Only mark as failed if not already successful
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Investment rejected: " + event.getRejectionReason());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            log.info("Payment marked as failed for rejected investment: {}", event.getInvestmentId());
        } else {
            log.warn("Cannot mark payment as failed - already successful: {}", payment.getId());
        }
    }
}
