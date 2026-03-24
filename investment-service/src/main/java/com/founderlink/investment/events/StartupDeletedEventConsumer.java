package com.founderlink.investment.events;

import java.util.List;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.founderlink.investment.entity.Investment;
import com.founderlink.investment.entity.InvestmentStatus;
import com.founderlink.investment.repository.InvestmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupDeletedEventConsumer {

    private final InvestmentRepository
            investmentRepository;

    @RabbitListener(
        queues = "${rabbitmq.startup.deleted.queue}")
    public void handleStartupDeletedEvent(
            StartupDeletedEvent event) {

        log.info("Received STARTUP_DELETED event " +
                "for startupId: {}",
                event.getStartupId());

        try {
        	
            // Find all investments
            // for deleted startup
        	
            List<Investment> investments =
                    investmentRepository
                            .findByStartupId(
                                    event.getStartupId());

            if (investments.isEmpty()) {
                log.info("No investments found " +
                        "for startupId: {}",
                        event.getStartupId());
                return;
            }

            // Update only PENDING and APPROVED
            // Leave COMPLETED and REJECTED
            
            investments.stream()
                    .filter(investment ->
                            investment.getStatus()
                                == InvestmentStatus.PENDING
                            ||
                            investment.getStatus()
                                == InvestmentStatus.APPROVED)
                    .forEach(investment -> {
                        investment.setStatus(
                            InvestmentStatus.STARTUP_CLOSED);
                        investmentRepository
                                .save(investment);
                        log.info("Investment id: {} " +
                                "marked as STARTUP_CLOSED",
                                investment.getId());
                    });

            log.info("Processed STARTUP_DELETED event " +
                    "for startupId: {} successfully",
                    event.getStartupId());

        } catch (Exception e) {
            log.error("Failed to process " +
                    "STARTUP_DELETED event " +
                    "for startupId: {} error: {}",
                    event.getStartupId(),
                    e.getMessage());
        }
    }
}