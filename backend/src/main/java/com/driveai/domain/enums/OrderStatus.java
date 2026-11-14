package com.driveai.domain.enums;

/**
 * State implementado mediante constantes con comportamiento.
 * Se conserva el mismo enum persistido: no requiere migrar ordenes existentes.
 */
public enum OrderStatus {
    PENDING {
        @Override
        public boolean canTransitionTo(OrderStatus target) {
            return target == CONFIRMED || target == CANCELLED;
        }
    },
    CONFIRMED {
        @Override
        public boolean canTransitionTo(OrderStatus target) {
            return target == CANCELLED;
        }
    },
    CANCELLED {
        @Override
        public boolean canTransitionTo(OrderStatus target) {
            return false;
        }
    };

    public abstract boolean canTransitionTo(OrderStatus target);

    public boolean isTerminal() {
        return this == CANCELLED;
    }
}
