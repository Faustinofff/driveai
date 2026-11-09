package com.driveai.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Consumidor de eventos de órdenes.
 *
 * <ul>
 *   <li><b>Idempotente:</b> el mismo {@code eventId} se procesa una sola vez.</li>
 *   <li><b>Mensajes inválidos:</b> se rechazan sin reencolar y terminan en la
 *       DLQ configurada en {@link MessagingConfig}.</li>
 *   <li><b>Fallos transitorios:</b> Spring AMQP reintenta (máx. 3) y luego el
 *       mensaje va a la DLQ.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventsListener {

    static final String CONSUMER_NAME = "order-events-consumer";

    private final ProcessedMessageService processedMessageService;

    @RabbitListener(queues = MessagingConfig.ORDER_QUEUE)
    public void onOrderEvent(OrderEvent event) {
        if (event == null
                || event.eventId() == null || event.eventId().isBlank()
                || event.orderId() == null
                || event.userId() == null
                || event.status() == null
                || event.totalAmount() == null
                || event.totalAmount().compareTo(BigDecimal.ZERO) < 0) {
            log.error("Mensaje de orden inválido; se envía a DLQ: {}", event);
            throw new AmqpRejectAndDontRequeueException("Evento de orden inválido");
        }

        boolean firstProcessing = processedMessageService.markIfFirst(event.eventId(), CONSUMER_NAME);
        if (!firstProcessing) {
            log.info("Evento duplicado ignorado (idempotencia): eventId={}", event.eventId());
            return;
        }

        // Efecto de negocio downstream (notificación/analítica). Si fallara,
        // Spring AMQP reintenta y finalmente deriva a la DLQ.
        log.info("Evento de orden procesado: eventId={} orderId={} userId={} status={} total={}",
                event.eventId(), event.orderId(), event.userId(), event.status(), event.totalAmount());
    }
}
