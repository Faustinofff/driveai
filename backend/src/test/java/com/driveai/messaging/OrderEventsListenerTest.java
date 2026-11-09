package com.driveai.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventsListenerTest {

    @Mock
    private ProcessedMessageService processedMessageService;

    @InjectMocks
    private OrderEventsListener listener;

    private static OrderEvent event(String eventId) {
        return new OrderEvent(eventId, "ORDER_CREATED", 5L, 42L,
                "CONFIRMED", new BigDecimal("22500.00"), Instant.now());
    }

    @Test
    void firstEventIsProcessedOnce() {
        when(processedMessageService.markIfFirst("evt-1", OrderEventsListener.CONSUMER_NAME)).thenReturn(true);

        assertThatCode(() -> listener.onOrderEvent(event("evt-1"))).doesNotThrowAnyException();
        verify(processedMessageService).markIfFirst("evt-1", OrderEventsListener.CONSUMER_NAME);
    }

    @Test
    void duplicateEventIsIgnoredWithoutSideEffects() {
        when(processedMessageService.markIfFirst("evt-1", OrderEventsListener.CONSUMER_NAME)).thenReturn(false);

        assertThatCode(() -> listener.onOrderEvent(event("evt-1"))).doesNotThrowAnyException();
    }

    @Test
    void nullEventGoesToDlq() {
        assertThatThrownBy(() -> listener.onOrderEvent(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verify(processedMessageService, never()).markIfFirst(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void invalidEventWithoutIdGoesToDlq() {
        assertThatThrownBy(() -> listener.onOrderEvent(event(" ")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void negativeAmountGoesToDlq() {
        OrderEvent invalid = new OrderEvent("evt-2", "ORDER_CREATED", 5L, 42L,
                "CONFIRMED", new BigDecimal("-1"), Instant.now());

        assertThatThrownBy(() -> listener.onOrderEvent(invalid))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void missingOrderIdGoesToDlq() {
        OrderEvent invalid = new OrderEvent("evt-3", "ORDER_CREATED", null, 42L,
                "CONFIRMED", BigDecimal.ONE, Instant.now());

        assertThatThrownBy(() -> listener.onOrderEvent(invalid))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }
}
