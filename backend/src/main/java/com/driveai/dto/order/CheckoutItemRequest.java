package com.driveai.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckoutItemRequest(
        @NotNull Long carId,
        @NotNull @Min(1) @Max(1000) Integer quantity) {
}