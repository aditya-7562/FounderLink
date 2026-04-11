package com.founderlink.payment.dlq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class DeadLetterQueueHandlerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DeadLetterLogRepository dlqLogRepository;

    @InjectMocks
    private DeadLetterQueueHandler deadLetterQueueHandler;

    @Test
    void handleDeadLetterMessage_Success_CreatedEvent() {
        String jsonMessage = "{\"eventType\":\"InvestmentCreatedEvent\",\"investmentId\":\"123\",\"investorId\":\"456\"}";

        deadLetterQueueHandler.handleDeadLetterMessage(jsonMessage);

        verify(dlqLogRepository, times(1)).save(any(DeadLetterLog.class));

        DeadLetterQueueHandler.DeadLetterQueueStats stats = deadLetterQueueHandler.getStats();
        assertThat(stats.totalMessagesReceived()).isEqualTo(1);
        assertThat(stats.processingErrors()).isZero();
    }

    @Test
    void handleDeadLetterMessage_Success_InferredRejectedEvent() {
        String jsonMessage = "{\"investmentId\":\"123\",\"rejectionReason\":\"bad\"}";

        deadLetterQueueHandler.handleDeadLetterMessage(jsonMessage);

        verify(dlqLogRepository).save(any(DeadLetterLog.class));
        DeadLetterQueueHandler.DeadLetterQueueStats stats = deadLetterQueueHandler.getStats();
        assertThat(stats.successfullyLogged()).isEqualTo(1);
    }

    @Test
    void handleDeadLetterMessage_ParsingError_IncrementsErrorCount() {
        String invalidJson = "invalid-json";

        deadLetterQueueHandler.handleDeadLetterMessage(invalidJson);

        verify(dlqLogRepository, never()).save(any(DeadLetterLog.class));

        DeadLetterQueueHandler.DeadLetterQueueStats stats = deadLetterQueueHandler.getStats();
        assertThat(stats.totalMessagesReceived()).isEqualTo(1);
        assertThat(stats.processingErrors()).isEqualTo(1);
    }

    @Test
    void handleDeadLetterMessage_RepositoryError_IncrementsErrorCount() {
        String jsonMessage = "{\"eventType\":\"TestEvent\"}";
        doThrow(new RuntimeException("DB Error")).when(dlqLogRepository).save(any());

        deadLetterQueueHandler.handleDeadLetterMessage(jsonMessage);

        DeadLetterQueueHandler.DeadLetterQueueStats stats = deadLetterQueueHandler.getStats();
        assertThat(stats.processingErrors()).isEqualTo(1);
    }

    @Test
    void handleDeadLetterMessage_MissingFields_UsesDefaults() {
        String jsonMessage = "{}";

        deadLetterQueueHandler.handleDeadLetterMessage(jsonMessage);

        verify(dlqLogRepository).save(any(DeadLetterLog.class));
        DeadLetterQueueHandler.DeadLetterQueueStats stats = deadLetterQueueHandler.getStats();
        assertThat(stats.successfullyLogged()).isEqualTo(1);
    }
}
