package com.driveai.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * El dueño del carrito se resuelve desde el JWT: el cliente no puede indicar
 * un sessionId ni un userId ajeno.
 */
public record CartAddRequest(
        @NotNull Long carId,
        @NotNull @Min(1) @Max(100) Integer quantity) {
}
