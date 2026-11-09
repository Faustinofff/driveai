package com.driveai.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Marca de mensajes ya procesados. El consumidor es idempotente: si recibe
 * dos veces el mismo messageId, la operación se ejecuta una sola vez.
 */
@Entity
@Table(name = "processed_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedMessage {

    @Id
    @Column(name = "message_id", length = 100)
    private String messageId;

    @Column(nullable = false, length = 100)
    private String consumer;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    void onCreate() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }
}
