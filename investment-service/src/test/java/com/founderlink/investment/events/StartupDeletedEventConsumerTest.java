package com.founderlink.investment.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.investment.entity.Investment;
import com.founderlink.investment.entity.InvestmentStatus;
import com.founderlink.investment.repository.InvestmentRepository;

@ExtendWith(MockitoExtension.class)
class StartupDeletedEventConsumerTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @InjectMocks
    private StartupDeletedEventConsumer consumer;

    @Test
    void handleStartupDeletedEvent_Success() {
        StartupDeletedEvent event = new StartupDeletedEvent(101L, 5L);
        Investment inv1 = new Investment();
        inv1.setId(1L);
        inv1.setStatus(InvestmentStatus.PENDING);
        
        Investment inv2 = new Investment();
        inv2.setId(2L);
        inv2.setStatus(InvestmentStatus.APPROVED);

        Investment inv3 = new Investment();
        inv3.setId(3L);
        inv3.setStatus(InvestmentStatus.COMPLETED);

        when(investmentRepository.findByStartupId(101L)).thenReturn(List.of(inv1, inv2, inv3));

        consumer.handleStartupDeletedEvent(event);

        verify(investmentRepository, times(2)).save(any(Investment.class));
        assert inv1.getStatus() == InvestmentStatus.STARTUP_CLOSED;
        assert inv2.getStatus() == InvestmentStatus.STARTUP_CLOSED;
        assert inv3.getStatus() == InvestmentStatus.COMPLETED;
    }

    @Test
    void handleStartupDeletedEvent_NoInvestments() {
        StartupDeletedEvent event = new StartupDeletedEvent(101L, 5L);
        when(investmentRepository.findByStartupId(101L)).thenReturn(List.of());

        consumer.handleStartupDeletedEvent(event);

        verify(investmentRepository, never()).save(any());
    }

    @Test
    void handleStartupDeletedEvent_Error() {
        StartupDeletedEvent event = new StartupDeletedEvent(101L, 5L);
        when(investmentRepository.findByStartupId(101L)).thenThrow(new RuntimeException("DB error"));

        // Should catch exception and log
        consumer.handleStartupDeletedEvent(event);
        
        verify(investmentRepository, never()).save(any());
    }
}
