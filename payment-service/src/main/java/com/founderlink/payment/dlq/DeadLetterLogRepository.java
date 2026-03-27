package com.founderlink.payment.dlq;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Dead Letter Log persistence.
 */
@Repository
public interface DeadLetterLogRepository extends JpaRepository<DeadLetterLog, Long> {

    /**
     * Find DLQ log by DLQ ID.
     */
    Optional<DeadLetterLog> findByDlqId(String dlqId);

    /**
     * Find all DLQ logs with given status.
     */
    List<DeadLetterLog> findByStatus(String status);

    /**
     * Find DLQ logs received after a certain time.
     */
    List<DeadLetterLog> findByReceivedAtAfter(LocalDateTime dateTime);

    /**
     * Find DLQ logs for specific event type.
     */
    List<DeadLetterLog> findByEventType(String eventType);

    /**
     * Find DLQ logs by investment ID.
     */
    List<DeadLetterLog> findByInvestmentId(String investmentId);

    /**
     * Count unreviewed DLQ messages.
     */
    long countByStatus(String status);
}
