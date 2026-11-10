package com.driveai.domain.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void pendingCanBeConfirmedOrCancelled() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.PENDING)).isFalse();
        assertThat(OrderStatus.PENDING.canTransitionTo(null)).isFalse();
    }

    @Test
    void confirmedCanOnlyBeCancelled() {
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PENDING)).isFalse();
    }

    @Test
    void cancelledIsTerminal() {
        assertThat(OrderStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PENDING)).isFalse();
        assertThat(OrderStatus.PENDING.isTerminal()).isFalse();
    }

    /** Matriz completa de la máquina de estados: 3 × 3 combinaciones. */
    @ParameterizedTest(name = "{0} → {1} permitido={2}")
    @CsvSource({
            "PENDING,   PENDING,   false",
            "PENDING,   CONFIRMED, true",
            "PENDING,   CANCELLED, true",
            "CONFIRMED, PENDING,   false",
            "CONFIRMED, CONFIRMED, false",
            "CONFIRMED, CANCELLED, true",
            "CANCELLED, PENDING,   false",
            "CANCELLED, CONFIRMED, false",
            "CANCELLED, CANCELLED, false"
    })
    void transitionMatrix(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }
}
