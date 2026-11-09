package com.driveai.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento publicado cuando se crea o cambia el estado de una orden.
 * `eventId` es la clave de idempotencia del consumidor.
 */
public record OrderEvent(
        String eventId,
        String eventType,
        Long orderId,
        Long userId,
        String status,
        BigDecimal totalAmount,
        Instant occurredAt) {
}
