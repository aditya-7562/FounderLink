package com.founderlink.messaging.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "messages",
        indexes = {
                @Index(name = "idx_messages_sender_created",   columnList = "sender_id, created_at"),
                @Index(name = "idx_messages_receiver_created", columnList = "receiver_id, created_at"),
                // Composite indexes for cursor-based conversation queries (both directions)
                @Index(name = "idx_conv_fwd", columnList = "sender_id, receiver_id, id"),
                @Index(name = "idx_conv_rev", columnList = "receiver_id, sender_id, id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
