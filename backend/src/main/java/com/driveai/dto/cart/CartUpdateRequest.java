package com.driveai.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartUpdateRequest(
        @NotNull @Min(1) @Max(100) Integer quantity) {
}