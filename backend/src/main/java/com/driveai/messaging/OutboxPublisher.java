package com.driveai.messaging;

import com.driveai.domain.model.OutboxEvent;
import com.driveai.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Publicador del patrón Transactional Outbox.
 *
 * <p>Si el broker está caído, el evento permanece PENDING y se reintenta en
 * el siguiente ciclo. Cuando el broker vuelve, el sistema se recupera solo y
 * no se pierden mensajes. El consumidor es idempotente, por lo que un envío
 * duplicado (por ejemplo si falla el marcado SENT) no duplica el efecto.
 *
 * <p>Los fallos técnicos (broker caído, timeout, reconexión) nunca descartan
 * el evento: se incrementa el contador de intentos y se mantiene PENDING.
 * Solo un payload que no se puede deserializar queda FAILED, porque reintentarlo
 * no puede tener éxito y requiere intervención manual.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.outbox.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        if (!enabled) {
            return;
        }
        List<OutboxEvent> pending = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        for (OutboxEvent event : pending) {
            try {
                OrderEvent orderEvent = objectMapper.readValue(event.getPayload(), OrderEvent.class);
                rabbitTemplate.convertAndSend(
                        MessagingConfig.ORDER_EXCHANGE,
                        MessagingConfig.ORDER_ROUTING_KEY,
                        orderEvent,
                        message -> {
                            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            message.getMessageProperties().setContentType("application/json");
                            message.getMessageProperties().setMessageId("outbox-" + event.getId());
                            return message;
                        });
                event.setStatus("SENT");
                event.setSentAt(Instant.now());
                event.setLastError(null);
            } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                // Payload corrupto: reintentar no puede funcionar. Se marca FAILED
                // para que un operador lo reprocese o corrija manualmente.
                int attempts = event.getAttempts() == null ? 0 : event.getAttempts();
                event.setAttempts(attempts + 1);
                event.setLastError(truncate(ex.getMessage()));
                event.setStatus("FAILED");
                log.error("Evento outbox {} tiene payload inválido; queda FAILED para reproceso manual",
                        event.getId(), ex);
            } catch (RuntimeException ex) {
                // Fallo técnico (broker caído, timeout, reconexión): se mantiene
                // PENDING y se reintenta en el próximo ciclo. No se descarta el
                // evento para no perder mensajes durante una caída prolongada.
                int attempts = event.getAttempts() == null ? 0 : event.getAttempts();
                event.setAttempts(attempts + 1);
                event.setLastError(truncate(ex.getMessage()));
                log.warn("Broker no disponible; evento outbox {} queda PENDING (intento {})",
                        event.getId(), event.getAttempts());
            }
            outboxRepository.save(event);
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
