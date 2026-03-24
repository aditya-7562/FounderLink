package com.founderlink.payment.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Audit log for payment operations.
 * Enables traceability and debugging of saga steps.
 */
@Entity
@Table(name = "payment_transaction_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false, length = 50)
    private String action;  // HOLD_SUCCESS, CAPTURE_SUCCESS, RELEASE_SUCCESS, CAPTURE_FAILED, etc.

    @Column(columnDefinition = "JSON")
    private String details;  // JSON details: error messages, gateway responses, etc.

    @Column(updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}
