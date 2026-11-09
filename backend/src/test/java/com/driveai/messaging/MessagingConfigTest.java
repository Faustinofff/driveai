package com.driveai.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class MessagingConfigTest {

    private final MessagingConfig config = new MessagingConfig();

    @Test
    void declaresDurableExchangeQueueAndDlqTopology() {
        DirectExchange exchange = config.orderExchange();
        Queue queue = config.orderQueue();
        Binding binding = config.orderBinding(queue, exchange);

        assertThat(exchange.getName()).isEqualTo(MessagingConfig.ORDER_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(queue.getName()).isEqualTo(MessagingConfig.ORDER_QUEUE);
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", MessagingConfig.DEAD_LETTER_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", MessagingConfig.DEAD_LETTER_ROUTING_KEY);
        assertThat(binding.getRoutingKey()).isEqualTo(MessagingConfig.ORDER_ROUTING_KEY);

        DirectExchange dlx = config.deadLetterExchange();
        Queue dlq = config.deadLetterQueue();
        Binding dlqBinding = config.deadLetterBinding(dlq, dlx);
        assertThat(dlx.getName()).isEqualTo(MessagingConfig.DEAD_LETTER_EXCHANGE);
        assertThat(dlq.getName()).isEqualTo(MessagingConfig.DEAD_LETTER_QUEUE);
        assertThat(dlqBinding.getRoutingKey()).isEqualTo(MessagingConfig.DEAD_LETTER_ROUTING_KEY);
    }

    @Test
    void usesJsonMessageConverter() {
        assertThat(config.jsonMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
