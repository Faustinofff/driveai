package com.driveai.messaging;

import com.driveai.domain.model.Order;
import com.driveai.domain.model.OutboxEvent;
import com.driveai.domain.enums.OrderStatus;
import com.driveai.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Test
    void recordsSerializedPendingEventInSameTransaction() {
        OutboxService service = new OutboxService(outboxRepository, new ObjectMapper().findAndRegisterModules());
        Order order = Order.builder()
                .id(5L)
                .userId(42L)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("22500.00"))
                .build();

        service.recordOrderEvent(order, "ORDER_CREATED");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("ORDER");
        assertThat(saved.getAggregateId()).isEqualTo(5L);
        assertThat(saved.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getPayload()).contains("\"orderId\":5", "\"userId\":42", "CONFIRMED");
    }
}
