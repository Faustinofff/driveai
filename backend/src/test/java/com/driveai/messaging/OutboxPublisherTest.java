package com.driveai.messaging;

import com.driveai.domain.model.OutboxEvent;
import com.driveai.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final String PAYLOAD = """
            {"eventId":"evt-1","eventType":"ORDER_CREATED","orderId":5,"userId":42,
             "status":"CONFIRMED","totalAmount":22500.00,"occurredAt":"2026-01-01T00:00:00Z"}
            """;

    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(outboxRepository, rabbitTemplate, new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(publisher, "enabled", true);
    }

    private OutboxEvent event(int attempts) {
        return OutboxEvent.builder()
                .id(5L)
                .aggregateType("ORDER")
                .aggregateId(5L)
                .eventType("ORDER_CREATED")
                .payload(PAYLOAD)
                .status("PENDING")
                .attempts(attempts)
                .build();
    }

    @Test
    void publishesPendingEventAndMarksItSent() {
        OutboxEvent event = event(0);
        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));

        publisher.publishPending();

        assertThat(event.getStatus()).isEqualTo("SENT");
        assertThat(event.getSentAt()).isNotNull();
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));
        verify(outboxRepository).save(event);
    }

    @Test
    void brokerOutageKeepsEventPendingForRetry() {
        OutboxEvent event = event(2);
        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        doThrow(new AmqpException("broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));

        publisher.publishPending();

        assertThat(event.getStatus()).isEqualTo("PENDING");
        assertThat(event.getAttempts()).isEqualTo(3);
        assertThat(event.getLastError()).contains("broker down");
    }

    @Test
    void brokerOutageKeepsEventPendingEvenAfterManyAttempts() {
        OutboxEvent event = event(100);
        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        doThrow(new AmqpException("still down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));

        publisher.publishPending();

        // Un fallo técnico nunca descarta el evento: sigue PENDING para reintentar
        // cuando el broker vuelva (requisito de recuperación ante caída).
        assertThat(event.getStatus()).isEqualTo("PENDING");
        assertThat(event.getAttempts()).isEqualTo(101);
    }

    @Test
    void corruptedPayloadIsMarkedFailed() {
        OutboxEvent event = event(0);
        event.setPayload("not-json");
        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));

        publisher.publishPending();

        assertThat(event.getStatus()).isEqualTo("FAILED");
        assertThat(event.getAttempts()).isEqualTo(1);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));
    }

    @Test
    void disabledPublisherDoesNothing() {
        ReflectionTestUtils.setField(publisher, "enabled", false);

        publisher.publishPending();

        verify(outboxRepository, never()).findTop50ByStatusOrderByCreatedAtAsc(anyString());
    }
}
