package com.founderlink.payment.dlq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadLetterQueueHandlerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DeadLetterLogRepository dlqLogRepository;

    @InjectMocks
    private DeadLetterQueueHandler deadLetterQueueHandler;

    @Test
    void handleDeadLetterMessage_Success() {
        String jsonMessage = "{\"eventType\":\"InvestmentCreatedEvent\",\"investmentId\":\"123\",\"investorId\":\"456\"}";

        deadLetterQueueHandler.handleDeadLetterMessage(jsonMessage);

        verify(dlqLogRepository, times(1)).save(any(DeadLetterLog.class));

        DeadLetterQueueHandler.DeadLetterQueueStats stats = deadLetterQueueHandler.getStats();
        assertEquals(1, stats.totalMessagesReceived());
        assertEquals(0, stats.processingErrors());
        assertEquals(1, stats.successfullyLogged());
    }

    @Test
    void handleDeadLetterMessage_ParsingError() {
        String invalidJson = "invalid-json";

        deadLetterQueueHandler.handleDeadLetterMessage(invalidJson);

        verify(dlqLogRepository, never()).save(any(DeadLetterLog.class));

        DeadLetterQueueHandler.DeadLetterQueueStats stats = deadLetterQueueHandler.getStats();
        assertEquals(1, stats.totalMessagesReceived());
        assertEquals(1, stats.processingErrors());
        assertEquals(0, stats.successfullyLogged());
    }
}
