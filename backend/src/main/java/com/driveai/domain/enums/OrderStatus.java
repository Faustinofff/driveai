package com.driveai.domain.enums;

/**
 * Estados de una orden y transiciones válidas.
 *
 * <p>Máquina de estados explícita (patrón State a nivel de dominio):
 * <pre>
 *   PENDING   → CONFIRMED | CANCELLED
 *   CONFIRMED → CANCELLED
 *   CANCELLED → (terminal)
 * </pre>
 * Cualquier otra transición es inválida y la capa de servicio responde 409.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == CANCELLED;
            case CANCELLED -> false;
        };
    }

    public boolean isTerminal() {
        return this == CANCELLED;
    }
}
