package com.driveai.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * El dueño de la orden se resuelve desde el JWT. `sessionId` ya no se acepta:
 * un cliente no puede operar sobre el carrito/orden de otro usuario.
 */
public record CheckoutRequest(
        @NotEmpty @Size(max = 50) List<@Valid CheckoutItemRequest> items) {
}
