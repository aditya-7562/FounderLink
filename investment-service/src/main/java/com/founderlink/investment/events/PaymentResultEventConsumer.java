package com.founderlink.investment.events;

import com.founderlink.investment.service.InvestmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResultEventConsumer {

    private final InvestmentService investmentService;

    @RabbitListener(queues = "${rabbitmq.payment.completed.queue}")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        investmentService.markCompletedFromPayment(event.getInvestmentId());
        log.info("PAYMENT_COMPLETED consumed for investmentId={}", event.getInvestmentId());
    }

    @RabbitListener(queues = "${rabbitmq.payment.failed.queue}")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        investmentService.markPaymentFailedFromPayment(event.getInvestmentId());
        log.info("PAYMENT_FAILED consumed for investmentId={}", event.getInvestmentId());
    }
}
