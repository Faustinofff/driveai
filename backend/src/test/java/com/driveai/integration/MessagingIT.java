package com.driveai.integration;

import com.driveai.domain.model.OutboxEvent;
import com.driveai.messaging.MessagingConfig;
import com.driveai.messaging.OrderEvent;
import com.driveai.messaging.OrderEventsListener;
import com.driveai.messaging.OutboxPublisher;
import com.driveai.repository.OutboxRepository;
import com.driveai.repository.ProcessedMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Pruebas de integración de mensajería con PostgreSQL y RabbitMQ reales.
 *
 * <p>Cubre los requisitos del eje C:
 * <ul>
 *   <li>Outbox transaccional publicado al broker y consumido.</li>
 *   <li>Consumidor idempotente ante mensajes duplicados.</li>
 *   <li>Mensaje inválido derivado a la Dead Letter Queue.</li>
 *   <li>Caída del broker: el evento queda PENDING y se recupera al volver.</li>
 * </ul>
 *
 * Ejecutar con {@code mvn verify -Pintegration} (requiere Docker).
 */
@SpringBootTest(properties = {
        "app.outbox.enabled=true",
        "app.outbox.publish-interval-ms=3600000"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MessagingIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new FixedPortRabbitMQContainer("rabbitmq:3.13-management-alpine", 55672);

    /**
     * Testcontainers recrea el contenedor al hacer {@code stop()}/{@code start()}.
     * Si el puerto host es aleatorio, la aplicación queda apuntando al puerto
     * viejo y nunca reconecta. Fijamos el puerto host para que la URL configurada
     * por {@code @ServiceConnection} siga siendo válida cuando el broker vuelve.
     */
    static class FixedPortRabbitMQContainer extends RabbitMQContainer {
        FixedPortRabbitMQContainer(String image, int hostPort) {
            super(image);
            addFixedExposedPort(hostPort, 5672);
        }
    }

    @Autowired
    private OutboxPublisher outboxPublisher;
    @Autowired
    private OutboxRepository outboxRepository;
    @Autowired
    private ProcessedMessageRepository processedMessageRepository;
    @Autowired
    private OrderEventsListener orderEventsListener;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private OrderEvent event(String eventId) {
        return new OrderEvent(
                eventId,
                "ORDER_CREATED",
                42L,
                7L,
                "CONFIRMED",
                new BigDecimal("24999.00"),
                Instant.now());
    }

    private OutboxEvent pendingOutbox(OrderEvent event) throws Exception {
        return outboxRepository.save(OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId(event.orderId())
                .eventType(event.eventType())
                .payload(objectMapper.writeValueAsString(event))
                .status("PENDING")
                .attempts(0)
                .build());
    }

    @Test
    @Order(1)
    void outboxPublishesToBrokerAndConsumerProcessesEvent() throws Exception {
        String eventId = "evt-" + UUID.randomUUID();
        OutboxEvent outbox = pendingOutbox(event(eventId));

        outboxPublisher.publishPending();

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            OutboxEvent stored = outboxRepository.findById(outbox.getId()).orElseThrow();
            assertThat(stored.getStatus()).isEqualTo("SENT");
            assertThat(stored.getSentAt()).isNotNull();
            assertThat(processedMessageRepository.existsById(eventId)).isTrue();
        });
    }

    @Test
    @Order(2)
    void duplicateEventIsProcessedOnlyOnce() {
        String eventId = "evt-dup-" + UUID.randomUUID();
        OrderEvent duplicate = event(eventId);

        orderEventsListener.onOrderEvent(duplicate);
        orderEventsListener.onOrderEvent(duplicate);
        orderEventsListener.onOrderEvent(duplicate);

        assertThat(processedMessageRepository.existsById(eventId)).isTrue();
        assertThat(processedMessageRepository.findAll().stream()
                .filter(message -> message.getMessageId().equals(eventId))
                .count()).isEqualTo(1);
    }

    @Test
    @Order(3)
    void invalidMessageIsRoutedToDeadLetterQueue() {
        OrderEvent invalid = new OrderEvent(null, "ORDER_CREATED", null, null, null, null, null);

        rabbitTemplate.convertAndSend(
                MessagingConfig.ORDER_EXCHANGE,
                MessagingConfig.ORDER_ROUTING_KEY,
                invalid);

        Message dead = await().atMost(Duration.ofSeconds(20))
                .until(() -> rabbitTemplate.receive(MessagingConfig.DEAD_LETTER_QUEUE, 1_000),
                        message -> message != null);

        assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).contains("ORDER_CREATED");
    }

    @Test
    @Order(4)
    void brokerOutageKeepsEventPendingAndRecoversWhenBrokerReturns() throws Exception {
        String eventId = "evt-recovery-" + UUID.randomUUID();
        OutboxEvent outbox = pendingOutbox(event(eventId));

        rabbit.stop();
        try {
            outboxPublisher.publishPending();
            OutboxEvent afterOutage = outboxRepository.findById(outbox.getId()).orElseThrow();
            assertThat(afterOutage.getStatus()).isEqualTo("PENDING");
            assertThat(afterOutage.getAttempts()).isGreaterThanOrEqualTo(1);
            assertThat(afterOutage.getLastError()).isNotBlank();
        } finally {
            rabbit.start();
        }

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            outboxPublisher.publishPending();
            assertThat(outboxRepository.findById(outbox.getId()).orElseThrow().getStatus())
                    .isEqualTo("SENT");
        });

        await().atMost(Duration.ofSeconds(30))
                .until(() -> processedMessageRepository.existsById(eventId));
    }
}
