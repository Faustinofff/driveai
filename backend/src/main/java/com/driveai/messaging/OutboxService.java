package com.driveai.messaging;

import com.driveai.domain.model.Order;
import com.driveai.domain.model.OutboxEvent;
import com.driveai.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Escribe eventos en la tabla outbox dentro de la misma transacción que la
 * orden. No publica directamente al broker: eso lo hace {@link OutboxPublisher}.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordOrderEvent(Order order, String eventType) {
        OrderEvent event = new OrderEvent(
                UUID.randomUUID().toString(),
                eventType,
                order.getId(),
                order.getUserId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                Instant.now());
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el evento de la orden", ex);
        }
        OutboxEvent outbox = OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId(order.getId())
                .eventType(eventType)
                .payload(payload)
                .status("PENDING")
                .attempts(0)
                .build();
        outboxRepository.save(outbox);
    }
}
