package com.driveai.domain.model;

import com.driveai.domain.enums.OrderStatus;
import com.driveai.exception.ConflictException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateTest {
    @ParameterizedTest
    @CsvSource({
            "PENDING, CONFIRMED, true",
            "PENDING, CANCELLED, true",
            "PENDING, PENDING, false",
            "CONFIRMED, CANCELLED, true",
            "CONFIRMED, CONFIRMED, false",
            "CONFIRMED, PENDING, false",
            "CANCELLED, PENDING, false",
            "CANCELLED, CONFIRMED, false",
            "CANCELLED, CANCELLED, false"
    })
    void entityEnforcesEveryTransition(OrderStatus from, OrderStatus to, boolean allowed) {
        Order order = Order.builder().status(from).build();
        if (allowed) {
            order.transitionTo(to);
            assertThat(order.getStatus()).isEqualTo(to);
        } else {
            assertThatThrownBy(() -> order.transitionTo(to)).isInstanceOf(ConflictException.class);
            assertThat(order.getStatus()).isEqualTo(from);
        }
    }

    @Test
    void nullTargetDoesNotChangeState() {
        Order order = Order.builder().status(OrderStatus.CONFIRMED).build();
        assertThatThrownBy(() -> order.transitionTo(null)).isInstanceOf(ConflictException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
