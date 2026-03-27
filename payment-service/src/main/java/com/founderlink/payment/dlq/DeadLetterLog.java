package com.founderlink.payment.dlq;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to persist dead letter queue messages for audit and manual review.
 */
@Entity
@Table(name = "dead_letter_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 36)
    private String dlqId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(length = 50)
    private String investmentId;

    @Column(length = 50)
    private String investorId;

    @Column(columnDefinition = "LONGTEXT")
    private String messagePayload;

    @Column(nullable = false, length = 20)
    private String status;  // RECEIVED, REVIEWED, RESOLVED, IGNORED

    @Column
    private String reviewNotes;

    @Column(updatable = false)
    private LocalDateTime receivedAt;

    @Column
    private LocalDateTime lastUpdated;

    @PrePersist
    protected void onCreate() {
        this.lastUpdated = LocalDateTime.now();
    }
}
